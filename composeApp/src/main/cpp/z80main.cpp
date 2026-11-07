#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <atomic>
#include <chrono>
#include <thread>
#include <deque>
#include <array>

#ifdef _MSC_VER
#define GCC_UNUSED
#else
#define GCC_UNUSED __attribute__((unused))
#endif

#include "z80.h"
#include "z80_macros.h"
#include "native-lib.h"
#include "watchers.h"
#include "patch.h"
#include "Abc80Sound.h"

/* 64Kb of RAM, first 16kb is ROM */
std::uint8_t memory[0x10000];
std::atomic<bool> g_abc800m_active{false};
static std::atomic<bool> g_abc800c_active{false};

static std::array<std::uint8_t, 0x4000> g_abc800m_video_ram{};
static std::array<std::uint8_t, 0x800> g_abc800m_character_ram{};
static std::array<std::uint8_t, 18> g_abc800m_crtc_registers{};
static std::uint8_t g_abc800m_crtc_address = 0;
static bool g_abc800m_keydtr = true;
static bool g_abc800m_character_fetch = false;
static std::atomic_flag g_abc800m_character_ram_lock = ATOMIC_FLAG_INIT;
static std::atomic_flag g_abc800m_crtc_lock = ATOMIC_FLAG_INIT;
static std::atomic_flag g_abc800m_video_ram_lock = ATOMIC_FLAG_INIT;
static std::deque<std::uint8_t> g_abc800m_keyboard;
static std::atomic<bool> g_abc800m_key_click{false};
static std::uint8_t g_abc800m_dart_register[2][8]{};
static std::uint8_t g_abc800m_dart_selected[2]{};
static bool g_abc800m_dart_expect_register[2]{};
static std::atomic<bool> g_abc800m_dart_rx_irq_enabled{false};
static std::atomic<bool> g_abc800m_dart_irq_pending{false};
static std::uint8_t g_abc800m_highres_control[2]{};
static std::atomic<bool> g_abc800_hr_enabled{true};

struct Abc800CtcChannel {
    std::uint8_t control = 0x03;
    unsigned constant = 256;
    unsigned counter = 256;
    unsigned prescaler_cycles = 0;
    bool waiting_constant = false;
    bool running = false;
    bool pending = false;
    bool in_service = false;
};
static std::array<Abc800CtcChannel, 4> g_abc800m_ctc{};
static std::uint8_t g_abc800m_ctc_vector = 0;
static std::uint64_t g_abc800m_ctc_cycles = 0;

static void abc800_ctc_advance() {
    const auto now = TOTAL_CYCLES;
    const auto elapsed = now >= g_abc800m_ctc_cycles ? now - g_abc800m_ctc_cycles : 0;
    g_abc800m_ctc_cycles = now;
    for (auto &channel : g_abc800m_ctc) {
        if (!channel.running || (channel.control & 0x40)) continue;
        const unsigned divisor = (channel.control & 0x20) ? 256 : 16;
        const auto cycles = channel.prescaler_cycles + elapsed;
        auto ticks = cycles / divisor;
        channel.prescaler_cycles = static_cast<unsigned>(cycles % divisor);
        if (ticks >= channel.counter) {
            ticks -= channel.counter;
            if (channel.control & 0x80) channel.pending = true;
            channel.counter = channel.constant - static_cast<unsigned>(ticks % channel.constant);
        } else {
            channel.counter -= static_cast<unsigned>(ticks);
        }
    }
}

static void abc800_ctc_write(int index, std::uint8_t value) {
    abc800_ctc_advance();
    auto &channel = g_abc800m_ctc[index];
    if (channel.waiting_constant) {
        channel.constant = value ? value : 256;
        channel.counter = channel.constant;
        channel.prescaler_cycles = 0;
        channel.waiting_constant = false;
        channel.control &= ~0x02;
        channel.running = (channel.control & 0x48) == 0;
    } else if (value & 1) {
        channel.control = value;
        channel.waiting_constant = (value & 0x04) != 0;
        if (value & 0x02) {
            channel.running = false;
            channel.prescaler_cycles = 0;
            channel.pending = false;
        }
        if (!(value & 0x80)) channel.pending = false;
    } else if (index == 0) {
        g_abc800m_ctc_vector = value & 0xf8;
    }
}

static inline std::size_t abc800_character_ram_mask() {
    return g_abc800c_active.load(std::memory_order_relaxed) ? 0x3ff : 0x7ff;
}

bool running = true;
std::atomic<bool> g_cpu_paused{false};
uint64_t g_last_interrupt_tstate = 0;
static std::atomic<int> runtime_cpu_frequency{3'000'000};

void abc80_write_sound(int value) {
    abc80_sound_write(value, TOTAL_CYCLES, runtime_cpu_frequency.load(std::memory_order_relaxed),
                      g_fast_speed.load(std::memory_order_relaxed));
}

void set_cpu_frequency(int cpu_frequency) {
    if (cpu_frequency > 0) {
        runtime_cpu_frequency.store(cpu_frequency, std::memory_order_relaxed);
    }
}

void add_time(libspectrum_dword time) {
    tStates += time;
    TOTAL_CYCLES += (uint64_t)time;
}

/* ABC80 keyboard: Z80-PIO port A (0x38). Bit 7 is the strobe, bits 0-6 the key code. */
static std::atomic<int> g_key_value{0};
static std::atomic<bool> g_key_irq_pending{false};
static std::atomic_flag g_key_lock = ATOMIC_FLAG_INIT;
struct KeyLock {
    KeyLock() { while (g_key_lock.test_and_set(std::memory_order_acquire)) std::this_thread::yield(); }
    ~KeyLock() { g_key_lock.clear(std::memory_order_release); }
};
struct Abc800Lock {
    explicit Abc800Lock(std::atomic_flag &lock) : lock_(lock) {
        while (lock_.test_and_set(std::memory_order_acquire)) std::this_thread::yield();
    }
    ~Abc800Lock() { lock_.clear(std::memory_order_release); }
    std::atomic_flag &lock_;
};
struct KeyPress { int code; bool held; };
static std::deque<KeyPress> g_key_queue;
static std::atomic<int> g_held_key{-1};
static bool g_current_key_held = false;
static int g_key_hold_frames = 0;
static constexpr libspectrum_byte KEYBOARD_PIO_VECTOR = 0x34;

/* ABC80 cassette: Z80-PIO port B interrupt, latched until the CPU accepts it. */
static std::atomic<int> g_cassette_vector{-1};
static std::atomic<bool> g_cassette_in_service{false};

void abc80_request_cassette_interrupt(int vector) {
    g_cassette_vector.store(vector, std::memory_order_relaxed);
}

void abc80_cassette_reti() {
    g_cassette_in_service.store(false, std::memory_order_relaxed);
}

void abc800_ctc_reti() {
    if (!g_abc800m_active.load(std::memory_order_relaxed)) return;
    for (auto &channel : g_abc800m_ctc) {
        if (channel.in_service) {
            channel.in_service = false;
            break;
        }
    }
}

void abc80_send_key(int code) {
    KeyLock lock;
    if (g_abc800m_active.load(std::memory_order_relaxed)) {
        if (g_abc800m_keyboard.size() < 64) {
            g_abc800m_keyboard.push_back(static_cast<std::uint8_t>(code));
            g_abc800m_key_click.store(true, std::memory_order_relaxed);
            if (g_abc800m_dart_rx_irq_enabled.load(std::memory_order_relaxed))
                g_abc800m_dart_irq_pending.store(true, std::memory_order_relaxed);
        }
        return;
    }
    if (g_key_queue.size() < 64) g_key_queue.push_back({code & 0x7F, false});
}

void abc80_press_key(int code) {
    if (g_abc800m_active.load(std::memory_order_relaxed)) {
        abc80_send_key(code);
        return;
    }
    KeyLock lock;
    code &= 0x7F;
    if (g_held_key.load() == code) {
        if (g_key_value.load() == (code | 0x80)) g_key_irq_pending.store(true);
        return;
    }
    if (g_key_queue.size() >= 64) {
        logd("ABC80 keyboard queue is full; physical key press dropped");
        return;
    }
    g_held_key.store(code);
    g_key_queue.push_back({code, true});
}

void abc80_release_key(int code) {
    if (g_abc800m_active.load(std::memory_order_relaxed)) return;
    int expected = code & 0x7F;
    g_held_key.compare_exchange_strong(expected, -1);
}

void abc80_release_all_keys() {
    g_held_key.store(-1);
    KeyLock lock;
    g_abc800m_keyboard.clear();
    g_abc800m_key_click.store(false, std::memory_order_relaxed);
}

int abc80_read_keyboard() {
    return g_key_value.load();
}

/* Called between instructions; delivers the pending keyboard interrupt once the CPU accepts it. */
bool abc80_try_keyboard_interrupt() {
    if (g_abc800m_active.load(std::memory_order_relaxed)) abc800_ctc_advance();
    if (!IFF1 || tStates == z80.interrupts_enabled_at) return false;
    if (g_abc800m_active.load(std::memory_order_relaxed)) {
        for (unsigned i = 0; i < g_abc800m_ctc.size(); ++i) {
            auto &channel = g_abc800m_ctc[i];
            if (channel.in_service) return false;
            if (!channel.pending) continue;
            const auto previous = g_im2_vector;
            g_im2_vector = static_cast<libspectrum_byte>(g_abc800m_ctc_vector | (i << 1));
            const bool taken = z80_interrupt() != 0;
            g_im2_vector = previous;
            if (taken) {
                channel.pending = false;
                channel.in_service = true;
            }
            return taken;
        }
    }
    if (g_abc800m_active.load(std::memory_order_relaxed) &&
        g_abc800m_dart_irq_pending.load(std::memory_order_relaxed)) {
        const libspectrum_byte previous = g_im2_vector;
        g_im2_vector = static_cast<libspectrum_byte>(
                (g_abc800m_dart_register[1][2] & 0xf8) | 0x04);
        const bool taken = z80_interrupt() != 0;
        g_im2_vector = previous;
        if (taken) g_abc800m_dart_irq_pending.store(false, std::memory_order_relaxed);
        return taken;
    }
    if (!g_key_irq_pending.load(std::memory_order_relaxed)) {
        const int vector = g_cassette_vector.load(std::memory_order_relaxed);
        if (vector < 0 || g_cassette_in_service.load(std::memory_order_relaxed)) return false;
        const libspectrum_byte previous = g_im2_vector;
        g_im2_vector = static_cast<libspectrum_byte>(vector);
        const bool taken = z80_interrupt() != 0;
        g_im2_vector = previous;
        if (taken) {
            g_cassette_vector.store(-1, std::memory_order_relaxed);
            g_cassette_in_service.store(true, std::memory_order_relaxed);
        }
        return taken;
    }
    const libspectrum_byte previous_vector = g_im2_vector;
    g_im2_vector = KEYBOARD_PIO_VECTOR;
    const bool accepted = z80_interrupt() != 0;
    g_im2_vector = previous_vector;
    if (accepted) g_key_irq_pending.store(false, std::memory_order_relaxed);
    return accepted;
}

/* Queued text uses a two-frame pulse; physical keys stay down for at least five frames or until release. */
static void abc80_keyboard_frame() {
    if (g_abc800m_active.load(std::memory_order_relaxed) &&
        g_abc800m_key_click.exchange(false, std::memory_order_relaxed)) {
        abc800_sound_key_click(TOTAL_CYCLES, runtime_cpu_frequency.load(std::memory_order_relaxed),
                               g_fast_speed.load(std::memory_order_relaxed));
    }
    if (g_key_value.load(std::memory_order_relaxed) != 0) {
        const int min_frames = (g_current_key_held || I == 0xFA) ? 5 : 2;
        if (g_key_hold_frames < min_frames) ++g_key_hold_frames;
        if (g_current_key_held &&
            g_held_key.load() == (g_key_value.load() & 0x7F)) return;
        if (g_key_hold_frames >= min_frames) {
            g_key_value.store(0, std::memory_order_relaxed);
            g_key_hold_frames = 0;
        }
        return;
    }
    KeyLock lock;
    if (!g_key_queue.empty()) {
        const auto code = g_key_queue.front();
        g_key_queue.pop_front();
        g_current_key_held = code.held;
        g_key_value.store(code.code | 0x80, std::memory_order_relaxed);
        g_key_irq_pending.store(true, std::memory_order_relaxed);
        g_key_hold_frames = 0;
    }
}

int run() {
    libspectrum_dword execute = g_t_states_per_frame;
    z80_reset(true);
    tStates = 0;
    g_last_interrupt_tstate = 0;
    abc80_sound_reset(TOTAL_CYCLES);

    auto frame_start = std::chrono::steady_clock::now();
    int loop_count = 0;

    while (running) {
        if (g_frozen.load(std::memory_order_acquire)) {
            g_cpu_paused.store(true, std::memory_order_release);
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            continue;
        }
        g_cpu_paused.store(false, std::memory_order_release);

        if (++loop_count >= 50) { // Every second at 50fps
            logd("PC: %04X, AF: %04X, SP: %04X", PC, AF, SP);
            loop_count = 0;
        }

        // event_next_event is the limit for this run
        z80_do_opcodes();
        abc80_sound_advance(TOTAL_CYCLES, runtime_cpu_frequency.load(std::memory_order_relaxed),
                            g_fast_speed.load(std::memory_order_relaxed),
                            g_abc800m_active.load(std::memory_order_relaxed));
        abc80_keyboard_frame();

        // Maintain long-term timing for tape pulses by keeping track of the overshoot
        // tStates is the relative counter used inside z80_do_opcodes.
        libspectrum_dword actual_run = tStates;
        execute = executeFor();
        if (actual_run >= event_next_event) {
            tStates = actual_run - event_next_event;
        } else {
            tStates = 0;
        }
        event_next_event = execute;

        if (!g_fast_speed) {
            const auto cpu_frequency = runtime_cpu_frequency.load(std::memory_order_relaxed);
            const auto frame_duration = std::chrono::nanoseconds(
                    static_cast<long long>(
                            (static_cast<double>(g_t_states_per_frame) * 1'000'000'000.0) /
                            cpu_frequency));
            auto frame_end = std::chrono::steady_clock::now();
            auto elapsed = std::chrono::duration_cast<std::chrono::nanoseconds>(
                    frame_end - frame_start);

            if (elapsed < frame_duration) {
                std::this_thread::sleep_for(frame_duration - elapsed);
            }
            frame_start += frame_duration;

            if (std::chrono::steady_clock::now() > frame_start + std::chrono::milliseconds(100)) {
                frame_start = std::chrono::steady_clock::now();
            }
        } else {
            frame_start = std::chrono::steady_clock::now();
        }
    }
    return 1;
}

void contend_read(libspectrum_word GCC_UNUSED address, libspectrum_dword time) {
    add_time(time);
}

void contend_read_no_mreq(libspectrum_word GCC_UNUSED address, libspectrum_dword time) {
    add_time(time);
}

void contend_write_no_mreq(libspectrum_word GCC_UNUSED address, libspectrum_dword time) {
    add_time(time);
}

libspectrum_byte readByte(libspectrum_word address) {
    add_time(3);
    libspectrum_byte value;
    if (get_patched_byte(address, &value)) {
        return value;
    }
    if (g_abc800m_active.load(std::memory_order_relaxed)) {
        if (address < 0x4000 && (!g_abc800m_keydtr || g_abc800m_character_fetch ||
                                  g_abc800m_highres_control[0] != 0 || g_abc800m_highres_control[1] != 0))
        {
            if (!g_abc800_hr_enabled.load(std::memory_order_relaxed)) return 0xff;
            Abc800Lock lock(g_abc800m_video_ram_lock);
            return g_abc800m_video_ram[address];
        }
        if (address >= 0x7800 && address < 0x8000 && !g_abc800m_character_fetch)
            return g_abc800m_character_ram[(address - 0x7800) & abc800_character_ram_mask()];
        if (address >= 0x7800 && address < 0x8000 &&
            !g_abc800_hr_enabled.load(std::memory_order_relaxed)) return 0xff;
    }
    return memory[address];
}

libspectrum_byte readByteInternal(libspectrum_word address) {
    check_and_run_read_watcher(address);
    libspectrum_byte value;
    if (get_patched_byte(address, &value)) {
        return value;
    }
    if (g_abc800m_active.load(std::memory_order_relaxed)) {
        g_abc800m_character_fetch = address >= 0x7800 && address < 0x8000;
        if (address < 0x4000 && (!g_abc800m_keydtr || g_abc800m_character_fetch ||
                                  g_abc800m_highres_control[0] != 0 || g_abc800m_highres_control[1] != 0))
        {
            if (!g_abc800_hr_enabled.load(std::memory_order_relaxed)) return 0xff;
            Abc800Lock lock(g_abc800m_video_ram_lock);
            return g_abc800m_video_ram[address];
        }
        if (address >= 0x7800 && address < 0x8000 && !g_abc800m_character_fetch)
            return g_abc800m_character_ram[(address - 0x7800) & abc800_character_ram_mask()];
        if (address >= 0x7800 && address < 0x8000 &&
            !g_abc800_hr_enabled.load(std::memory_order_relaxed)) return 0xff;
    }
    return memory[address];
}

void writeByte(libspectrum_word address, libspectrum_byte b) {
    add_time(3);
    writeByteInternal(address, b);
}

void writeByteInternal(libspectrum_word address, libspectrum_byte b) {
    check_and_run_write_watcher(address, b);
    if (g_abc800m_active.load(std::memory_order_relaxed)) {
        if (address < 0x4000) {
            if (g_abc800_hr_enabled.load(std::memory_order_relaxed)) {
                Abc800Lock lock(g_abc800m_video_ram_lock);
                g_abc800m_video_ram[address] = b;
            }
            return;
        }
        if (address >= 0x7800 && address < 0x8000) {
            Abc800Lock lock(g_abc800m_character_ram_lock);
            g_abc800m_character_ram[(address - 0x7800) & abc800_character_ram_mask()] = b;
            return;
        }
        if (address < 0x8000) return;
    }
    memory[address] = b;
}

extern "C" {
void abc800m_clear_text_screen();

static void abc800_set_enabled(bool enabled, bool color_model) {
    g_abc800m_active.store(enabled, std::memory_order_relaxed);
    g_abc800c_active.store(enabled && color_model, std::memory_order_relaxed);
    {
        Abc800Lock lock(g_abc800m_video_ram_lock);
        g_abc800m_video_ram.fill(0);
        std::memset(g_abc800m_highres_control, 0, sizeof(g_abc800m_highres_control));
    }
    abc800m_clear_text_screen();
    {
        Abc800Lock lock(g_abc800m_crtc_lock);
        g_abc800m_crtc_registers.fill(0);
        g_abc800m_crtc_address = 0;
    }
    g_abc800m_keydtr = true;
    g_abc800m_character_fetch = false;
    {
        KeyLock lock;
        g_abc800m_keyboard.clear();
        g_abc800m_key_click.store(false, std::memory_order_relaxed);
    }
    std::memset(g_abc800m_dart_register, 0, sizeof(g_abc800m_dart_register));
    std::memset(g_abc800m_dart_selected, 0, sizeof(g_abc800m_dart_selected));
    std::memset(g_abc800m_dart_expect_register, 0, sizeof(g_abc800m_dart_expect_register));
    g_abc800m_dart_rx_irq_enabled.store(false, std::memory_order_relaxed);
    g_abc800m_dart_irq_pending.store(false, std::memory_order_relaxed);
    g_abc800m_ctc = {};
    g_abc800m_ctc_vector = 0;
    g_abc800m_ctc_cycles = TOTAL_CYCLES;
}

void abc800m_set_enabled(bool enabled) {
    abc800_set_enabled(enabled, false);
}

void abc800c_set_enabled(bool enabled) {
    abc800_set_enabled(enabled, true);
}

void abc800_set_hr_enabled(bool enabled) {
    Abc800Lock lock(g_abc800m_video_ram_lock);
    g_abc800_hr_enabled.store(enabled, std::memory_order_relaxed);
    g_abc800m_video_ram.fill(0);
    std::memset(g_abc800m_highres_control, 0, sizeof(g_abc800m_highres_control));
}

void abc800m_clear_text_screen() {
    Abc800Lock crtc_lock(g_abc800m_crtc_lock);
    Abc800Lock character_ram_lock(g_abc800m_character_ram_lock);
    g_abc800m_character_ram.fill(0x20);
    g_abc800m_crtc_registers[14] = g_abc800m_crtc_registers[12];
    g_abc800m_crtc_registers[15] = g_abc800m_crtc_registers[13];
}

std::uint8_t abc800m_read_port(int port) {
    port &= 0xff;
    if (port >= 0x20 && port <= 0x2f) {
        const int offset = port & 3;
        const int channel = offset >= 2 ? 1 : 0;
        if ((offset & 1) == 0) {
            KeyLock lock;
            if (channel == 1 && !g_abc800m_keyboard.empty()) {
                const auto code = g_abc800m_keyboard.front();
                g_abc800m_keyboard.pop_front();
                if (!g_abc800m_keyboard.empty() &&
                    g_abc800m_dart_rx_irq_enabled.load(std::memory_order_relaxed))
                    g_abc800m_dart_irq_pending.store(true, std::memory_order_relaxed);
                return code;
            }
            return 0;
        }
        const auto selected = g_abc800m_dart_selected[channel];
        if (selected == 0) {
            KeyLock lock;
            const int receive_ready = channel == 1 && !g_abc800m_keyboard.empty();
            return static_cast<std::uint8_t>(0x04 | static_cast<int>(receive_ready));
        }
        const auto value = g_abc800m_dart_register[channel][selected];
        g_abc800m_dart_selected[channel] = 0;
        return value;
    }
    if (!g_abc800c_active.load(std::memory_order_relaxed) &&
        (port == 0x31 || port == 0x35 || port == 0x39 || port == 0x3d)) {
        Abc800Lock lock(g_abc800m_crtc_lock);
        return g_abc800m_crtc_registers[g_abc800m_crtc_address % g_abc800m_crtc_registers.size()];
    }
    if (port >= 0x60 && port <= 0x7f) {
        abc800_ctc_advance();
        return static_cast<std::uint8_t>(g_abc800m_ctc[port & 3].counter);
    }
    if ((port & 0xe7) == 0x05) {
        abc800_sound_toggle(TOTAL_CYCLES, runtime_cpu_frequency.load(std::memory_order_relaxed),
                            g_fast_speed.load(std::memory_order_relaxed));
        return 0xff;
    }
    return 0xff;
}

bool abc800m_owns_port_read(int port) {
    port &= 0xff;
    return (port >= 0x20 && port <= 0x2f) ||
           (!g_abc800c_active.load(std::memory_order_relaxed) &&
            (port == 0x31 || port == 0x35 || port == 0x39 || port == 0x3d)) ||
           (port >= 0x60 && port <= 0x7f) || (port & 0xe7) == 0x05 || port == 0x06;
}

void abc800m_write_port(int port, int value) {
    port &= 0xff;
    value &= 0xff;
    if (port >= 0x20 && port <= 0x2f) {
        const int offset = port & 3;
        const int channel = offset >= 2 ? 1 : 0;
        if ((offset & 1) == 0) return;
        if (g_abc800m_dart_expect_register[channel]) {
            const auto selected = g_abc800m_dart_selected[channel];
            g_abc800m_dart_register[channel][selected] = static_cast<std::uint8_t>(value);
            g_abc800m_dart_expect_register[channel] = false;
            g_abc800m_dart_selected[channel] = 0;
            if (channel == 1 && selected == 5) {
                // DART /DTRB drives KEYDTR: asserting WR5 bit 7 selects video RAM.
                g_abc800m_keydtr = (value & 0x80) == 0;
            }
            if (channel == 1 && selected == 1) {
                const bool rx_irq_enabled = ((value >> 3) & 3) != 0;
                g_abc800m_dart_rx_irq_enabled.store(rx_irq_enabled, std::memory_order_relaxed);
                KeyLock lock;
                if (rx_irq_enabled && !g_abc800m_keyboard.empty())
                    g_abc800m_dart_irq_pending.store(true, std::memory_order_relaxed);
            }
        } else {
            const auto selected = static_cast<std::uint8_t>(value & 7);
            g_abc800m_dart_selected[channel] = selected;
            g_abc800m_dart_expect_register[channel] = selected != 0;
        }
        return;
    }
    if (!g_abc800c_active.load(std::memory_order_relaxed) &&
        (port == 0x38 || port == 0x3c)) {
        Abc800Lock lock(g_abc800m_crtc_lock);
        g_abc800m_crtc_address = static_cast<std::uint8_t>(value & 0x1f);
        return;
    }
    if (!g_abc800c_active.load(std::memory_order_relaxed) &&
        (port == 0x39 || port == 0x3d)) {
        Abc800Lock lock(g_abc800m_crtc_lock);
        if (g_abc800m_crtc_address < g_abc800m_crtc_registers.size())
            g_abc800m_crtc_registers[g_abc800m_crtc_address] = static_cast<std::uint8_t>(value);
        return;
    }
    if ((port & 0xe7) == 0x06 || (port & 0xe7) == 0x07) {
        if (!g_abc800_hr_enabled.load(std::memory_order_relaxed)) return;
        Abc800Lock lock(g_abc800m_video_ram_lock);
        g_abc800m_highres_control[port & 1] = static_cast<std::uint8_t>(value);
        return;
    }
    if (port >= 0x60 && port <= 0x7f) {
        abc800_ctc_write(port & 3, static_cast<std::uint8_t>(value));
        return;
    }
}

void abc800m_copy_text_screen(std::uint8_t *destination) {
    Abc800Lock crtc_lock(g_abc800m_crtc_lock);
    Abc800Lock lock(g_abc800m_character_ram_lock);
    const int start = ((g_abc800m_crtc_registers[12] & 0x3f) << 8) |
                      g_abc800m_crtc_registers[13];
    for (int i = 0; i < 80 * 24; ++i)
        destination[i] = g_abc800m_character_ram[(start + i) & 0x7ff];
}

void abc800c_copy_text_screen(std::uint8_t *destination) {
    Abc800Lock lock(g_abc800m_character_ram_lock);
    for (int row = 0; row < 24; ++row) {
        for (int column = 0; column < 40; ++column) {
            int offset = ((row & 0x07) * 0x80) + column;
            if (row & 0x08) offset += 0x28;
            if (row & 0x10) offset += 0x50;
            destination[row * 40 + column] = g_abc800m_character_ram[offset & 0x3ff];
        }
    }
}

void abc800m_copy_high_resolution_screen(std::uint8_t *destination) {
    Abc800Lock lock(g_abc800m_video_ram_lock);
    std::memcpy(destination, g_abc800m_video_ram.data(), 64 * 240);
}

int abc800m_high_resolution_control() {
    Abc800Lock lock(g_abc800m_video_ram_lock);
    return static_cast<int>(g_abc800m_highres_control[0]) |
           (static_cast<int>(g_abc800m_highres_control[1]) << 8);
}

int abc800m_cursor_position() {
    Abc800Lock lock(g_abc800m_crtc_lock);
    if (g_abc800m_crtc_registers[10] & 0x20) return -1;
    const int start = ((g_abc800m_crtc_registers[12] & 0x3f) << 8) |
                      g_abc800m_crtc_registers[13];
    const int cursor = ((g_abc800m_crtc_registers[14] & 0x3f) << 8) |
                       g_abc800m_crtc_registers[15];
    const int offset = (cursor - start) & 0x7ff;
    return offset < 80 * 24 ? offset : -1;
}

int abc800m_cursor_raster_range() {
    Abc800Lock lock(g_abc800m_crtc_lock);
    const int start = g_abc800m_crtc_registers[10] & 0x1f;
    const int end = g_abc800m_crtc_registers[11] & 0x1f;
    return start | (end << 8);
}
}

libspectrum_byte readport(libspectrum_word port) {
    add_time(4);
    return static_cast<libspectrum_byte>(onReadPort(port, port >> 8));
}

void writeport(libspectrum_word port, libspectrum_byte b) {
    add_time(4);
    onWritePort(port, b);
}

void z80_debugger_variables_init() {}
