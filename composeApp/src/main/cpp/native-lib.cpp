#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <atomic>
#include <thread>
#include <cstring>
#include <cstdint>
#include <algorithm>
#include <mutex>

#ifdef _WIN32
#define NOMINMAX
#include <windows.h>
#endif

#ifdef __ANDROID__

#include <android/bitmap.h>

#endif

#include "z80.h"
#include "z80_macros.h"
#include "native-lib.h"

#define ZX_SPECTRUM_SCREEN_START 0x4000
#define ZX_SPECTRUM_PIXEL_DATA_SIZE 0x1800
#define ZX_SPECTRUM_ATTRIBUTE_START 0x5800
#define ZX_SPECTRUM_ATTRIBUTE_SIZE 768

int run();

extern std::uint8_t memory[0x10000];

std::atomic<bool> g_frozen(false);
std::atomic<bool> g_enable_logging(true);
std::atomic<bool> g_on_z80_main_loop_enabled(false);
std::atomic<bool> g_fast_speed(false);
std::atomic<bool> g_screen_invalidated(false);
std::atomic<bool> g_is_testing(false);
std::atomic<bool> g_single_step(false);

jclass g_native_lib_class = nullptr;
jobject g_native_lib_object = nullptr;
jmethodID g_disassemble_method = nullptr;
jmethodID g_on_z80_main_loop_method = nullptr;
JavaVM *g_vm = nullptr;

static jobject g_update_listener = nullptr;
static jobject g_tstate_listener = nullptr;
static jmethodID g_onUpdateMethodID = nullptr;
static jmethodID g_on_tstate_method_id = nullptr;
static jobject g_executeFor_listener = nullptr;
static jmethodID g_onExecuteForMethodID = nullptr;

static std::uint8_t s_screen_copy[ZX_SPECTRUM_PIXEL_DATA_SIZE];

static jint attach_current_thread(JNIEnv **env) {
#ifdef __ANDROID__
    return g_vm->AttachCurrentThread(env, nullptr);
#else
    return g_vm->AttachCurrentThread(reinterpret_cast<void **>(env), nullptr);
#endif
}

static uint16_t read16(const unsigned char *p) {
    return p[0] | (p[1] << 8);
}

static uint32_t read32(const unsigned char *p) {
    return p[0] | (p[1] << 8) | (p[2] << 16) | (p[3] << 24);
}

std::atomic<bool> g_emulator_running(false);

extern "C" {
unsigned int executeFor() {
    if (!g_executeFor_listener || !g_onExecuteForMethodID || !g_vm) return g_t_states_per_frame;
    JNIEnv *env = nullptr;
    if (attach_current_thread(&env) != JNI_OK) return g_t_states_per_frame;
    jint value = env->CallIntMethod(g_executeFor_listener, g_onExecuteForMethodID);
    return value > 0 ? (unsigned int) value : g_t_states_per_frame;
}

int onReadPort(int port, int hi) {
    int port8 = port & 0xFF;
    // Minimal "no disk" status for the DOS ROM's floppy controller: bit7/bit0 set (ready), bit1 clear.
    if (port8 == 1) return 0x81;
    return (port8 <= 7) ? 0x00 : 0xFF;
}

void onWritePort(int port, int value) {
    // Port writes ignored as CP/M I/O is HLE'd via BIOS intercepts
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_registerInstanceNative(JNIEnv *env, jobject instance) {
    if (g_native_lib_object) env->DeleteGlobalRef(g_native_lib_object);
    g_native_lib_object = env->NewGlobalRef(instance);
    jclass clazz = env->GetObjectClass(instance);
    g_disassemble_method = env->GetMethodID(clazz, "disassemble", "(I)Ljava/lang/String;");
}

JNIEXPORT jbyteArray JNICALL
Java_com_aboveware_aboveabc80_NativeLib_getMemoryNative(JNIEnv *env, jobject) {
    jbyteArray result = env->NewByteArray(0x10000);
    env->SetByteArrayRegion(result, 0, 0x10000, (jbyte *) memory);
    return result;
}

JNIEXPORT jboolean JNICALL
Java_com_aboveware_aboveabc80_NativeLib_isVideoMemoryDirtyNative(JNIEnv *env, jobject) {
    if (g_screen_invalidated.exchange(false) ||
        std::memcmp(s_screen_copy, &memory[ZX_SPECTRUM_SCREEN_START],
                    ZX_SPECTRUM_PIXEL_DATA_SIZE) != 0) {
        std::memcpy(s_screen_copy, &memory[ZX_SPECTRUM_SCREEN_START], ZX_SPECTRUM_PIXEL_DATA_SIZE);
        return JNI_TRUE;
    }
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_aboveware_aboveabc80_NativeLib_isFlashingNative(JNIEnv *env, jobject) {
    for (int i = 0; i < ZX_SPECTRUM_ATTRIBUTE_SIZE; ++i)
        if (memory[ZX_SPECTRUM_ATTRIBUTE_START + i] & 0x80)return JNI_TRUE;
    return JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_startEmulatorNative(JNIEnv *env, jobject,
                                                          jint cpu_frequency) {
    if (g_emulator_running) return;
    g_emulator_running = true;
    std::memcpy(s_screen_copy, &memory[ZX_SPECTRUM_SCREEN_START], ZX_SPECTRUM_PIXEL_DATA_SIZE);
    z80_init(cpu_frequency);
    set_cpu_frequency(cpu_frequency);
    std::thread(run).detach();
}

static void renderInternal(const std::uint8_t *screen_data, const std::uint8_t *attribute_data,
                           std::uint32_t *screen_array, int width, int scale_factor, bool flash) {
    std::uint32_t colors[] = {0xFF000000, 0xFFD70000, 0xFF0000D7, 0xFFD700D7, 0xFF00D700,
                              0xFFD7D700, 0xFF00D7D7, 0xFFD7D7D7, 0xFF000000, 0xFFFF0000,
                              0xFF0000FF, 0xFFFF00FF, 0xFF00FF00, 0xFFFFFF00, 0xFF00FFFF,
                              0xFFFFFFFF};
    for (int char_y = 0; char_y < 24; ++char_y) {
        for (int char_x = 0; char_x < 32; ++char_x) {
            std::uint8_t attr = attribute_data[(char_y * 32) + char_x];
            bool is_flash = (attr & 0x80) != 0;
            int bright = (attr & 0x40) >> 3;
            int paper_idx = bright | ((attr >> 3) & 0x07);
            int ink_idx = bright | (attr & 0x07);
            std::uint32_t ink = colors[ink_idx], paper = colors[paper_idx];
            if (flash && is_flash) std::swap(ink, paper);
            for (int y_in_char = 0; y_in_char < 8; ++y_in_char) {
                int y = (char_y * 8) + y_in_char;
                int pixel_addr_offset = ((y & 0xC0) << 5) | ((y & 0x07) << 8) | ((y & 0x38) << 2);
                std::uint8_t pixel_data = screen_data[pixel_addr_offset + char_x];
                int dest_y_base = y * scale_factor * width, dest_x_base = char_x * 8 * scale_factor;
                for (int bit = 0; bit < 8; ++bit) {
                    std::uint32_t color = (pixel_data & (1 << (7 - bit))) ? ink : paper;
                    int dest_x = dest_x_base + (bit * scale_factor);
                    for (int sy = 0; sy < scale_factor; ++sy)
                        for (int sx = 0; sx < scale_factor; ++sx)
                            screen_array[dest_y_base + (sy * width) + dest_x + sx] = color;
                }
            }
        }
    }
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_renderToBitmapNative(JNIEnv *env, jobject, jobject bitmap,
                                                           jboolean flash) {
#ifdef __ANDROID__
    AndroidBitmapInfo info;
    void *pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0 ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888 ||
        AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0)
        return;
    renderInternal(&memory[ZX_SPECTRUM_SCREEN_START], &memory[ZX_SPECTRUM_ATTRIBUTE_START],
                   static_cast<std::uint32_t *>(pixels), info.width, info.width / 256, flash);
    AndroidBitmap_unlockPixels(env, bitmap);
#endif
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_renderToBitmapNativeDesktop(JNIEnv *env, jobject,
                                                                  jintArray pixels,
                                                                  jboolean flash) {
    jsize len = env->GetArrayLength(pixels);
    jint *body = static_cast<jint *>(env->GetPrimitiveArrayCritical(pixels, nullptr));
    if (!body) return;
    int width = (len >= 512 * 384) ? 512 : 256;
    renderInternal(&memory[ZX_SPECTRUM_SCREEN_START], &memory[ZX_SPECTRUM_ATTRIBUTE_START],
                   reinterpret_cast<std::uint32_t *>(body), width, width / 256, flash);
    env->ReleasePrimitiveArrayCritical(pixels, body, 0);
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_renderScreenToBitmapNative(JNIEnv *env, jobject,
                                                                 jbyteArray screenData,
                                                                 jobject bitmap, jboolean flash) {
    if (env->GetArrayLength(screenData) < 6912) return;
    jbyte *data = env->GetByteArrayElements(screenData, nullptr);
    if (!data) return;
#ifdef __ANDROID__
    AndroidBitmapInfo info;
    void *pixels;
    if (AndroidBitmap_getInfo(env, bitmap, &info) >= 0 &&
        info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 &&
        AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0) {
        renderInternal(reinterpret_cast<const std::uint8_t *>(data),
                       reinterpret_cast<const std::uint8_t *>(data) + 6144,
                       static_cast<std::uint32_t *>(pixels), info.width, info.width / 256, flash);
        AndroidBitmap_unlockPixels(env, bitmap);
    }
#endif
    env->ReleaseByteArrayElements(screenData, data, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_renderScreenToBitmapNativeDesktop(JNIEnv *env, jobject,
                                                                        jbyteArray screenData,
                                                                        jintArray pixels,
                                                                        jboolean flash) {
    if (env->GetArrayLength(screenData) < 6912) return;
    jbyte *screenBody = env->GetByteArrayElements(screenData, nullptr);
    jint *pixelBody = static_cast<jint *>(env->GetPrimitiveArrayCritical(pixels, nullptr));
    if (screenBody && pixelBody) {
        int width = (env->GetArrayLength(pixels) >= 512 * 384) ? 512 : 256;
        renderInternal(reinterpret_cast<const std::uint8_t *>(screenBody),
                       reinterpret_cast<const std::uint8_t *>(screenBody) + 6144,
                       reinterpret_cast<std::uint32_t *>(pixelBody), width, width / 256, flash);
    }
    if (pixelBody) env->ReleasePrimitiveArrayCritical(pixels, pixelBody, 0);
    if (screenBody) env->ReleaseByteArrayElements(screenData, screenBody, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_copyToMemoryNative(JNIEnv *env, jobject, jint address,
                                                         jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    std::memcpy(&memory[address], bytes, len);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setZ80RegistersNative(JNIEnv *env, jobject, jobject regs) {
    jclass clazz = env->GetObjectClass(regs);
    auto getShort = [&](const char *name) {
        jfieldID fid = env->GetFieldID(clazz, name, "S");
        return fid ? env->GetShortField(regs, fid) : (jshort) 0;
    };
    auto getByte = [&](const char *name) {
        jfieldID fid = env->GetFieldID(clazz, name, "B");
        return fid ? env->GetByteField(regs, fid) : (jbyte) 0;
    };
    auto getInt = [&](const char *name) {
        jfieldID fid = env->GetFieldID(clazz, name, "I");
        return fid ? env->GetIntField(regs, fid) : (jint) 0;
    };
    auto getLong = [&](const char *name) {
        jfieldID fid = env->GetFieldID(clazz, name, "J");
        return fid ? env->GetLongField(regs, fid) : (jlong) 0;
    };

    z80.af.w = getShort("af");
    z80.bc.w = getShort("bc");
    z80.de.w = getShort("de");
    z80.hl.w = getShort("hl");
    z80.af_.w = getShort("af2");
    z80.bc_.w = getShort("bc2");
    z80.de_.w = getShort("de2");
    z80.hl_.w = getShort("hl2");
    z80.ix.w = getShort("ix");
    z80.iy.w = getShort("iy");
    z80.sp.w = getShort("sp");
    z80.pc.w = getShort("pc");
    z80.i = (uint8_t) getByte("i");

    uint8_t r_full = (uint8_t) getByte("r");
    z80.r = r_full & 0x7F;
    z80.r7 = r_full & 0x80;

    z80.iff1 = (uint8_t) getByte("iff1");
    z80.iff2 = (uint8_t) getByte("iff2");
    z80.im = (uint8_t) getByte("im");

    z80.total_cycles = (uint64_t) getLong("totalCycles");

    tStates = (libspectrum_dword)(z80.total_cycles % g_t_states_per_frame);
    g_last_interrupt_tstate = z80.total_cycles - tStates;

    z80.halted = (getByte("flags") & 0x01);
    z80.memptr.w = getShort("memPtr");

    z80.iff2_read = 0;
    z80.interrupts_enabled_at = (libspectrum_signed_dword) -1;
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setRegisterANative(JNIEnv *env, jobject, jint value) {
    z80.af.b.h = static_cast<uint8_t>(value & 0xFF);
    // Flags (af.b.l) are preserved entirely to avoid side effects in BDOS logic.
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setRegisterHLNative(JNIEnv *env, jobject, jint value) {
    z80.hl.w = static_cast<uint16_t>(value & 0xFFFF);
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setRegisterLNative(JNIEnv *env, jobject, jint value) {
    z80.hl.b.l = static_cast<uint8_t>(value & 0xFF);
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setRamNative(JNIEnv *env, jobject, jint address,
                                                   jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);
    if (bytes) {
        std::memcpy(&memory[address & 0xFFFF], bytes,
                    std::min((jsize) (0x10000 - (address & 0xFFFF)), len));
        if (address == 0x4000) g_screen_invalidated = true;
        env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setOnExecuteForListenerNative(JNIEnv *env, jobject,
                                                                    jobject listener) {
    if (g_executeFor_listener) env->DeleteGlobalRef(g_executeFor_listener);
    g_executeFor_listener = listener ? env->NewGlobalRef(listener) : nullptr;
    if (g_executeFor_listener)
        g_onExecuteForMethodID = env->GetMethodID(env->GetObjectClass(g_executeFor_listener),
                                                  "onExecuteFor", "()I");
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_freezeNative(JNIEnv *env, jobject, jboolean freeze) {
    g_frozen = (freeze == JNI_TRUE);
}
JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_enableLoggingNative(JNIEnv *env, jobject, jboolean enable) {
    g_enable_logging = (enable == JNI_TRUE);
}
JNIEXPORT void JNICALL Java_com_aboveware_aboveabc80_NativeLib_stepNative(JNIEnv *env, jobject) {
    g_single_step = true;
    g_frozen = false;
}
JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_enableOnZ80MainLoopNative(JNIEnv *env, jobject,
                                                                jboolean enable) {
    g_on_z80_main_loop_enabled = (enable == JNI_TRUE);
}
JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setFastSpeedNative(JNIEnv *env, jobject, jboolean fast) {
    g_fast_speed = (fast == JNI_TRUE);
}
JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setCpuFrequencyNative(JNIEnv *env, jobject,
                                                            jint frequency) {
    set_cpu_frequency(frequency);
}
JNIEXPORT jboolean JNICALL
Java_com_aboveware_aboveabc80_NativeLib_isFastSpeedNative(JNIEnv *env, jobject) {
    return (jboolean) g_fast_speed;
}
JNIEXPORT jlong JNICALL
Java_com_aboveware_aboveabc80_NativeLib_getTStatesNative(JNIEnv *env, jobject) {
    return (jlong) z80.total_cycles;
}
JNIEXPORT jstring JNICALL
Java_com_aboveware_aboveabc80_NativeLib_disassembleNative(JNIEnv *env, jobject, jint pc) {
    return env->NewStringUTF("NOP");
}
JNIEXPORT jbyteArray JNICALL
Java_com_aboveware_aboveabc80_NativeLib_getProcessorStateNative(JNIEnv *env, jobject) {
    jbyteArray result = env->NewByteArray(sizeof(processor));
    if (result) env->SetByteArrayRegion(result, 0, sizeof(processor), (jbyte *) &z80);
    return result;
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_updateCPUNative(JNIEnv *env, jobject, jobject cpu_obj) {
    jclass clazz = env->GetObjectClass(cpu_obj);
    auto setInt = [&](const char *name, int val) {
        env->SetIntField(cpu_obj, env->GetFieldID(clazz, name, "I"), val);
    };
    auto setShort = [&](const char *name, short val) {
        env->SetShortField(cpu_obj, env->GetFieldID(clazz, name, "S"), val);
    };
    auto setLong = [&](const char *name, long long val) {
        env->SetLongField(cpu_obj, env->GetFieldID(clazz, name, "J"), val);
    };

    setInt("af", z80.af.w);
    setInt("bc", z80.bc.w);
    setInt("de", z80.de.w);
    setInt("hl", z80.hl.w);
    setInt("af_", z80.af_.w);
    setInt("bc_", z80.bc_.w);
    setInt("de_", z80.de_.w);
    setInt("hl_", z80.hl_.w);
    setInt("ix", z80.ix.w);
    setInt("iy", z80.iy.w);
    setShort("i", z80.i);
    setInt("r", z80.r);
    setShort("r7", z80.r7);
    setInt("sp", z80.sp.w);
    setInt("pc", z80.pc.w);
    setInt("memptr", z80.memptr.w);
    setInt("iff2_read", z80.iff2_read);
    setShort("iff1", z80.iff1);
    setShort("iff2", z80.iff2);
    setShort("im", z80.im);
    setInt("halted", z80.halted);
    setLong("totalCycles", z80.total_cycles);
    setShort("q", z80.q);
    setLong("interrupts_enabled_at", z80.interrupts_enabled_at);
}

JNIEXPORT jstring JNICALL
Java_com_aboveware_aboveabc80_NativeLib_getProcessorStateStringNative(JNIEnv *env, jobject) {
    char buffer[1024];
    snprintf(buffer, sizeof(buffer),
             "AF=%04X BC=%04X DE=%04X HL=%04X\n"
             "AF'=%04X BC'=%04X DE'=%04X HL'=%04X\n"
             "IX=%04X IY=%04X SP=%04X PC=%04X\n"
             "I=%02X R=%02X IFF1=%d IFF2=%d IM=%d HALTED=%d\n"
             "Total Cycles=%llu MEMPTR=%04X",
             z80.af.w, z80.bc.w, z80.de.w, z80.hl.w,
             z80.af_.w, z80.bc_.w, z80.de_.w, z80.hl_.w,
             z80.ix.w, z80.iy.w, z80.sp.w, z80.pc.w,
             z80.i, (uint8_t) ((z80.r & 0x7f) | (z80.r7 & 0x80)),
             z80.iff1, z80.iff2, z80.im, z80.halted,
             (unsigned long long)z80.total_cycles, z80.memptr.w);
    return env->NewStringUTF(buffer);
}

JNIEXPORT void JNICALL
Java_com_aboveware_aboveabc80_NativeLib_setProcessorStateNative(JNIEnv *env, jobject,
                                                              jbyteArray state) {
    jbyte *buffer = env->GetByteArrayElements(state, nullptr);
    if (buffer) {
        std::memcpy(&z80, buffer, sizeof(processor));

        // Sync timing variables to prevent hang on restore
        tStates = (libspectrum_dword)(z80.total_cycles % g_t_states_per_frame);
        g_last_interrupt_tstate = z80.total_cycles - tStates;
        event_next_event = g_t_states_per_frame; // Reset next event limit

        env->ReleaseByteArrayElements(state, buffer, JNI_ABORT);
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *) {
    g_vm = vm;
    init_watchers_lock();
    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) return -1;
    jclass localClass = env->FindClass("com/aboveware/aboveabc80/NativeLib");
    if (localClass) g_native_lib_class = (jclass) env->NewGlobalRef(localClass);
    return JNI_VERSION_1_6;
}
}
