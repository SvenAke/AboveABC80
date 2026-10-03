#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <atomic>
#include <thread>
#include <cstring>
#include <cstdint>
#include <algorithm>
#include <chrono>
#include <ctime>
#include <cstdarg>
#include <cstdio>
#include <mutex>

#ifdef _WIN32
#define NOMINMAX
#include <windows.h>
#endif

#ifdef __ANDROID__

#include <android/log.h>

#endif

#include "watchers.h"
#include "z80.h"
#include "native-lib.h"

// Struct to hold watcher object and its specific method ID
struct WatcherInfo {
    jobject watcher_ref;
    jmethodID method_id;
};

// Use plain arrays for memory addresses 0-65535 to avoid std::map issues in JNI
// Each element is a vector of watchers for that address.
static std::vector<WatcherInfo> g_memory_read_watchers[65536];
static std::vector<WatcherInfo> g_memory_write_watchers[65536];

#ifdef _WIN32
static CRITICAL_SECTION g_watchers_cs;
static bool g_watchers_cs_initialized = false;
static void lock_watchers() {
    if (!g_watchers_cs_initialized) return;
    EnterCriticalSection(&g_watchers_cs);
}
static void unlock_watchers() {
    if (!g_watchers_cs_initialized) return;
    LeaveCriticalSection(&g_watchers_cs);
}
void init_watchers_lock() {
    if (!g_watchers_cs_initialized) {
        InitializeCriticalSection(&g_watchers_cs);
        g_watchers_cs_initialized = true;
    }
}
#else
static std::recursive_mutex g_watchers_mutex;

static void lock_watchers() { g_watchers_mutex.lock(); }

static void unlock_watchers() { g_watchers_mutex.unlock(); }

void init_watchers_lock() {}

#endif

extern std::uint8_t memory[0x10000];

// Helper for AttachCurrentThread which differs between Android and Desktop
static jint attach_current_thread(JNIEnv **env) {
#ifdef __ANDROID__
    return g_vm->AttachCurrentThread(env, nullptr);
#else
    return g_vm->AttachCurrentThread(reinterpret_cast<void **>(env), nullptr);
#endif
}

std::string disassemble(int pc) {
    JNIEnv *env;
    if (attach_current_thread(&env) != JNI_OK) return "";

    if (g_native_lib_object == nullptr || g_disassemble_method == nullptr) return "";

    auto result = (jstring) env->CallObjectMethod(g_native_lib_object, g_disassemble_method, pc);
    if (!result) return "";

    const char *str = env->GetStringUTFChars(result, nullptr);
    std::string cpp_str = str; // Create a copy
    env->ReleaseStringUTFChars(result, str);
    env->DeleteLocalRef(result);

    return cpp_str;
}

void logd(const char *format, ...) {
    if (g_enable_logging) {
        auto now = std::chrono::system_clock::now();
        auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()) %
                  1000;
        std::time_t timer = std::chrono::system_clock::to_time_t(now);
        struct std::tm bt{};

#ifdef _WIN32
        localtime_s(&bt, &timer);
#else
        localtime_r(&timer, &bt);
#endif

        char time_str[32];
        std::strftime(time_str, sizeof(time_str), "%T", &bt);

        char msg_buffer[1024];
        va_list args;
        va_start(args, format);
        std::vsnprintf(msg_buffer, sizeof(msg_buffer), format, args);
        va_end(args);

        char final_buffer[2048];
        std::snprintf(final_buffer, sizeof(final_buffer), "%s.%03lld: %s", time_str, ms.count(),
                      msg_buffer);

#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s", final_buffer);
#else
        std::printf("Native: %s\n", final_buffer);
        std::fflush(stdout);
#endif
    }
}

void hexdump(const char *desc, const void *addr, int len) {
    const auto *pc = (const unsigned char *) addr;
    char line_buffer[128];

    if (desc != nullptr) logd("%s (%d bytes):", desc, len);

    for (int i = 0; i < len; i += 16) {
        int offset = 0;
        // Address
        offset += std::sprintf(line_buffer + offset, "%04x: ", i);

        // Hex values
        for (int j = 0; j < 16; j++) {
            if (i + j < len)
                offset += std::sprintf(line_buffer + offset, "%02x ", pc[i + j]);
            else
                offset += std::sprintf(line_buffer + offset, "   ");
            if (j == 7) offset += std::sprintf(line_buffer + offset, " ");
        }

        offset += std::sprintf(line_buffer + offset, " |");

        // ASCII representation
        for (int j = 0; j < 16; j++) {
            if (i + j < len) {
                unsigned char c = pc[i + j];
                offset += std::sprintf(line_buffer + offset, "%c", (c >= 32 && c <= 126) ? c : '.');
            }
        }
        std::sprintf(line_buffer + offset, "|");

        logd("%s", line_buffer);
    }
}

extern "C" {

JNIEXPORT void JNICALL
Java_com_aboveware_abovecpm_NativeLib_addMemoryReadWatcherNative(
        JNIEnv *env,
        jobject /* this */,
        jint address,
        jobject watcher) {

    jobject global_watcher = env->NewGlobalRef(watcher);
    if (!global_watcher) return;

    jclass watcher_class = env->GetObjectClass(watcher);
    if (!watcher_class) {
        env->DeleteGlobalRef(global_watcher);
        return;
    }

    jmethodID method_id = env->GetMethodID(watcher_class, "onRead", "(I)Z");
    if (!method_id) {
        env->DeleteGlobalRef(global_watcher);
        return; // Method not found
    }

    lock_watchers();
    g_memory_read_watchers[address & 0xFFFF].push_back({global_watcher, method_id});
    unlock_watchers();
}

JNIEXPORT void JNICALL
Java_com_aboveware_abovecpm_NativeLib_removeMemoryReadWatcherNative(
        JNIEnv *env,
        jobject,
        jint address,
        jobject watcher) {

    if (!watcher) return;
    lock_watchers();
    auto &watchers = g_memory_read_watchers[address & 0xFFFF];
    for (auto watcher_it = watchers.begin(); watcher_it != watchers.end(); ++watcher_it) {
        if (env->IsSameObject(watcher_it->watcher_ref, watcher)) {
            env->DeleteGlobalRef(watcher_it->watcher_ref);
            watchers.erase(watcher_it);
            break;
        }
    }
    unlock_watchers();
}

JNIEXPORT void JNICALL
Java_com_aboveware_abovecpm_NativeLib_addMemoryWriteWatcherNative(
        JNIEnv *env,
        jobject /* this */,
        jint address,
        jobject watcher) {

    jobject global_watcher = env->NewGlobalRef(watcher);
    if (!global_watcher) return;

    jclass watcher_class = env->GetObjectClass(watcher);
    if (!watcher_class) {
        env->DeleteGlobalRef(global_watcher);
        return;
    }

    jmethodID method_id = env->GetMethodID(watcher_class, "onWrite", "(ISSZ)V");
    if (!method_id) {
        env->DeleteGlobalRef(global_watcher);
        return; // Method not found
    }

    lock_watchers();
    g_memory_write_watchers[address & 0xFFFF].push_back({global_watcher, method_id});
    unlock_watchers();
}

JNIEXPORT void JNICALL
Java_com_aboveware_abovecpm_NativeLib_removeMemoryWriteWatcherNative(
        JNIEnv *env,
        jobject /* this */,
        jint address,
        jobject watcher) {

    if (!watcher) return;
    lock_watchers();
    auto &watchers = g_memory_write_watchers[address & 0xFFFF];
    for (auto watcher_it = watchers.begin(); watcher_it != watchers.end(); ++watcher_it) {
        if (env->IsSameObject(watcher_it->watcher_ref, watcher)) {
            env->DeleteGlobalRef(watcher_it->watcher_ref);
            watchers.erase(watcher_it);
            break;
        }
    }
    unlock_watchers();
}

} // extern "C"

bool check_and_run_read_watcher(int address) {
    std::vector<WatcherInfo> to_call;
    lock_watchers();
    to_call = g_memory_read_watchers[address & 0xFFFF];
    unlock_watchers();

    if (!to_call.empty()) {
        JNIEnv *env;
        if (attach_current_thread(&env) != JNI_OK) return false;

        bool was_called = true;
        std::vector<WatcherInfo> to_remove;

        for (auto const &info: to_call) {
            jboolean remove_watcher = env->CallBooleanMethod(info.watcher_ref, info.method_id,
                                                             address);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
                continue;
            }
            if (remove_watcher == JNI_TRUE) {
                to_remove.push_back(info);
            }
        }

        if (!to_remove.empty()) {
            lock_watchers();
            auto &watchers = g_memory_read_watchers[address & 0xFFFF];
            for (auto const &info: to_remove) {
                auto watcher_it = std::find_if(watchers.begin(), watchers.end(),
                                               [&](const WatcherInfo &wi) {
                                                   return env->IsSameObject(wi.watcher_ref,
                                                                            info.watcher_ref);
                                               });
                if (watcher_it != watchers.end()) {
                    env->DeleteGlobalRef(watcher_it->watcher_ref);
                    watchers.erase(watcher_it);
                }
            }
            unlock_watchers();
        }

        return was_called;
    }
    return false;
}

void check_and_run_write_watcher(int address, libspectrum_byte new_value) {
    std::vector<WatcherInfo> to_call;
    lock_watchers();
    to_call = g_memory_write_watchers[address & 0xFFFF];
    unlock_watchers();

    if (!to_call.empty()) {
        JNIEnv *env;
        if (attach_current_thread(&env) != JNI_OK) return;

        libspectrum_byte before_value = memory[address];
        bool changed = before_value != new_value;

        logd("Running %zu write watcher(s) for address %04x with value %02x (before: %02x)",
             to_call.size(), address, new_value, before_value);
        for (auto const &info: to_call) {
            env->CallVoidMethod(info.watcher_ref, info.method_id, address,
                                static_cast<jshort>(new_value), static_cast<jshort>(before_value),
                                changed);
            if (env->ExceptionCheck()) {
                env->ExceptionDescribe();
                env->ExceptionClear();
            }
        }
    }
}
