#ifndef ABOVEABC80_SOUND_H
#define ABOVEABC80_SOUND_H

#include <cstdint>

constexpr int ABC80_SOUND_SAMPLE_RATE = 44100;
void abc80_sound_reset(std::uint64_t cycles);
void abc80_sound_advance(std::uint64_t cycles, int cpu_frequency, bool muted);
void abc80_sound_write(int value, std::uint64_t cycles, int cpu_frequency, bool muted);
void abc80_sound_read(float *samples, int count, bool muted);
void abc80_write_sound(int value);

#endif
