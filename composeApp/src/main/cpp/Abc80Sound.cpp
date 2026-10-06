#include "Abc80Sound.h"
#include "sn76477.h"
#include <algorithm>
#include <array>
#include <atomic>
#include <thread>

namespace {
constexpr std::size_t capacity = ABC80_SOUND_SAMPLE_RATE / 10;
constexpr int oversampling = SN76477_OVERSAMPLING;
std::array<short, capacity> pending{};
std::size_t head = 0;
std::size_t size = 0;
std::atomic_flag queue_lock = ATOMIC_FLAG_INIT;
struct QueueLock {
    QueueLock() {
        while (queue_lock.test_and_set(std::memory_order_acquire)) std::this_thread::yield();
    }
    ~QueueLock() { queue_lock.clear(std::memory_order_release); }
};
std::uint64_t last_cycles = 0;
double fractional_sample = 0;
int sample_sum = 0;
int sample_count = 0;
}

void abc80_sound_reset(std::uint64_t cycles) {
    sn76477_reset();
    last_cycles = cycles;
    fractional_sample = 0;
    sample_sum = sample_count = 0;
    QueueLock lock;
    head = size = 0;
}

void abc80_sound_advance(std::uint64_t cycles, int cpu_frequency, bool muted) {
    const auto elapsed = cycles >= last_cycles ? cycles - last_cycles : 0;
    last_cycles = cycles;
    if (muted) {
        fractional_sample = 0;
        sample_sum = sample_count = 0;
        QueueLock lock;
        head = size = 0;
        return;
    }
    // MUSIK.BAS inhibits the chip for only six microseconds: shorter than one
    // output sample. Integrate those pulses before downsampling to 44.1 kHz.
    const double exact = elapsed * static_cast<double>(ABC80_SOUND_SAMPLE_RATE * oversampling) /
                         cpu_frequency + fractional_sample;
    auto remaining = static_cast<std::uint64_t>(exact);
    fractional_sample = exact - remaining;
    std::array<short, 1024> buffer{};
    while (remaining > 0) {
        const auto count = static_cast<std::size_t>(std::min<std::uint64_t>(remaining, buffer.size()));
        sn76477_render(buffer.data(), static_cast<int>(count));
        QueueLock lock;
        for (std::size_t i = 0; i < count; ++i) {
            sample_sum += buffer[i];
            if (++sample_count < oversampling) continue;
            const short output = static_cast<short>(sample_sum / oversampling);
            sample_sum = sample_count = 0;
            if (size == capacity) {
                head = (head + 1) % capacity;
                --size;
            }
            pending[(head + size) % capacity] = output;
            ++size;
        }
        remaining -= count;
    }
}

void abc80_sound_write(int value, std::uint64_t cycles, int cpu_frequency, bool muted) {
    // Render the old pin state up to the OUT instruction before changing it.
    abc80_sound_advance(cycles, cpu_frequency, muted);
    sn76477_write_port(value);
}

void abc80_sound_read(float *samples, int count, bool muted) {
    QueueLock lock;
    if (muted) head = size = 0;
    for (int i = 0; i < count; ++i) {
        samples[i] = size > 0 ? pending[head] / 32768.0f : 0.0f;
        if (size > 0) {
            head = (head + 1) % capacity;
            --size;
        }
    }
}
