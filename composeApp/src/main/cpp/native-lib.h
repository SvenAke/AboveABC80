//
// Created by svena on 2025-11-28.
//

#ifndef ABOVE_CPM_NATIVE_LIB_H
#define ABOVE_CPM_NATIVE_LIB_H

#include <jni.h>

#ifdef __ANDROID__

#include <android/log.h>

#endif

#define LOG_TAG "NativeAboveABC80"

#ifdef __cplusplus
// C++ specific headers and declarations
#include <atomic>

void logd(const char *format, ...);

void hexdump(const char *desc, const void *addr, int len);
void set_cpu_frequency(int cpu_frequency);

void init_watchers_lock();

extern jclass g_native_lib_class;
extern jobject g_native_lib_object;
extern jmethodID g_disassemble_method;
extern JavaVM *g_vm;

extern std::atomic<bool> g_frozen;
extern std::atomic<bool> g_enable_logging;
extern std::atomic<bool> g_on_z80_main_loop_enabled;
extern std::atomic<bool> g_fast_speed;
extern std::atomic<bool> g_is_testing;
extern std::atomic<bool> g_single_step;

#endif // __cplusplus


#ifdef __cplusplus
extern "C" {
#endif

// Declarations visible to both C and C++

#ifndef __cplusplus
// C specific headers and declarations
#include <stdatomic.h>
#include <stdbool.h>
extern atomic_bool g_frozen;
extern atomic_bool g_enable_logging;
extern atomic_bool g_on_z80_main_loop_enabled;
extern atomic_bool g_fast_speed;
void hexdump(const char* desc, const void* addr, int len);
#endif

int onReadPort(int port, int value);
int abc80_read_keyboard();
void abc80_request_cassette_interrupt(int vector);
void abc80_cassette_reti();
void abc80_send_key(int code);
void abc80_press_key(int code);
void abc80_release_key(int code);
void abc80_release_all_keys();
void onWritePort(int port, int value);
void onUpdate();
unsigned int executeFor();
void onZ80MainLoop(int pc);
void onTState(int tStates, int pc);

#ifdef __cplusplus
}
#endif // __cplusplus

#endif //ABOVE_CPM_NATIVE_LIB_H
