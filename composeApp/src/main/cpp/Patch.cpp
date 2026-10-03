#include <jni.h>
#include <map>
#include <cstdint>
#include <vector>
#include "z80.h"
#include "z80_macros.h"
#include "patch.h"

// Forward declaration of the memory array from z80main.cpp
extern std::uint8_t memory[0x10000];

// Use a simple array for patches to avoid std::map issues and improve performance
static uint8_t g_patch_values[65536];
static bool g_patch_active[65536];

extern "C" JNIEXPORT jint JNICALL
Java_com_aboveware_abovecpm_NativeLib_peekMemory(
        JNIEnv *env,
        jobject /* this */,
        jint address) {
    if (address == -1) {
        return (jint) (TOTAL_CYCLES - g_last_interrupt_tstate);
    }
    uint16_t addr = static_cast<uint16_t>(address & 0xFFFF);
    if (g_patch_active[addr]) {
        return g_patch_values[addr];
    }
    return memory[addr];
}

// JNI function to add or remove a patch. A value < 0 removes the patch.
extern "C" JNIEXPORT void JNICALL
Java_com_aboveware_abovecpm_NativeLib_patchMemory(
        JNIEnv *env,
        jobject /* this */,
        jint address,
        jint value) {
    uint16_t addr = static_cast<uint16_t>(address & 0xFFFF);
    if (value < 0) {
        g_patch_active[addr] = false;
    } else {
        g_patch_values[addr] = static_cast<uint8_t>(value);
        g_patch_active[addr] = true;
    }
}

// JNI function to clear all patches
extern "C" JNIEXPORT void JNICALL
Java_com_aboveware_abovecpm_NativeLib_clearPatches(
        JNIEnv *env,
        jobject /* this */) {
    for (int i = 0; i < 65536; ++i) {
        g_patch_active[i] = false;
    }
}

// C-callable function to get a patched byte. Returns true if a patch was found.
extern "C" bool get_patched_byte(libspectrum_word address, libspectrum_byte *value) {
    if (g_patch_active[address]) {
        *value = g_patch_values[address];
        return true;
    }
    return false;
}
