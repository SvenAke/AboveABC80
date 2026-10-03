#include <cerrno>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <ctime>
#include <atomic>
#include <chrono>
#include <thread>

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
static std::atomic<int> runtime_cpu_frequency{4'000'000};

void set_cpu_frequency(int cpu_frequency) {
    if (cpu_frequency > 0) {
        runtime_cpu_frequency.store(cpu_frequency, std::memory_order_relaxed);
    }
}

void add_time(libspectrum_dword time) {
    tStates += time;
    TOTAL_CYCLES += (uint64_t)time;
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
