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
#include <mutex>

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

/* 64Kb of RAM, first 16kb is ROM */
std::uint8_t memory[0x10000];

bool running = true;
uint64_t g_last_interrupt_tstate = 0;
static std::atomic<int> runtime_cpu_frequency{3'000'000};

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
static std::deque<int> g_key_queue;
static int g_key_hold_frames = 0;
static constexpr libspectrum_byte KEYBOARD_PIO_VECTOR = 0x34;

void abc80_send_key(int code) {
    KeyLock lock;
    if (g_key_queue.size() < 64) g_key_queue.push_back(code & 0x7F);
}

int abc80_read_keyboard() {
    return g_key_value.load(std::memory_order_relaxed);
}

/* Called between instructions; delivers the pending keyboard interrupt once the CPU accepts it. */
bool abc80_try_keyboard_interrupt() {
    if (!g_key_irq_pending.load(std::memory_order_relaxed)) return false;
    if (!IFF1 || tStates == z80.interrupts_enabled_at) return false;
    const libspectrum_byte previous_vector = g_im2_vector;
    g_im2_vector = KEYBOARD_PIO_VECTOR;
    const bool accepted = z80_interrupt() != 0;
    g_im2_vector = previous_vector;
    if (accepted) g_key_irq_pending.store(false, std::memory_order_relaxed);
    return accepted;
}

/* Called once per emulated frame: holds each key for two frames, then releases it. */
static void abc80_keyboard_frame() {
    if (g_key_value.load(std::memory_order_relaxed) != 0) {
        if (++g_key_hold_frames >= 2) {
            g_key_value.store(0, std::memory_order_relaxed);
            g_key_hold_frames = 0;
        }
        return;
    }
    KeyLock lock;
    if (!g_key_queue.empty()) {
        const int code = g_key_queue.front();
        g_key_queue.pop_front();
        g_key_value.store(code | 0x80, std::memory_order_relaxed);
        g_key_irq_pending.store(true, std::memory_order_relaxed);
        g_key_hold_frames = 0;
    }
}

int run() {
    libspectrum_dword execute = g_t_states_per_frame;
    z80_reset(true);
    tStates = 0;
    g_last_interrupt_tstate = 0;

    auto frame_start = std::chrono::steady_clock::now();
    int loop_count = 0;

    while (running) {
        if (g_frozen) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            continue;
        }

        if (++loop_count >= 50) { // Every second at 50fps
            logd("PC: %04X, AF: %04X, SP: %04X", PC, AF, SP);
            loop_count = 0;
        }

        // event_next_event is the limit for this run
        z80_do_opcodes();
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
    return memory[address];
}

libspectrum_byte readByteInternal(libspectrum_word address) {
    check_and_run_read_watcher(address);
    libspectrum_byte value;
    if (get_patched_byte(address, &value)) {
        return value;
    }
    return memory[address];
}

void writeByte(libspectrum_word address, libspectrum_byte b) {
    add_time(3);
    writeByteInternal(address, b);
}

void writeByteInternal(libspectrum_word address, libspectrum_byte b) {
    check_and_run_write_watcher(address, b);
    memory[address] = b;
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
