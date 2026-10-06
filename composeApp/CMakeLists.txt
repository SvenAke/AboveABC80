cmake_minimum_required(VERSION 3.22.1)

# 1. Force CMake to NOT use sh.exe even if it's in the PATH (fixes the "Running" error)
if (WIN32)
    set(CMAKE_SH "CMAKE_SH-NOTFOUND")
endif ()

# 2. Skip the linker check during compiler detection to avoid failures
# when a full executable environment isn't ready yet.
set(CMAKE_TRY_COMPILE_TARGET_TYPE STATIC_LIBRARY)

# 3. Initialize project without languages first to avoid the "Running" crash
project(aboveabc80 LANGUAGES NONE)

# 4. Enable C and C++ explicitly.
enable_language(C CXX)

# 5. Handle JNI for Desktop and Android
if (NOT ANDROID)
    # Try to find JNI without requiring AWT
    find_package(JNI)

    if (JNI_FOUND)
        message(STATUS "JNI found: ${JNI_INCLUDE_DIRS}")
        include_directories(${JNI_INCLUDE_DIRS})
    elseif (DEFINED JNI_INCLUDE_DIRS)
        message(STATUS "Using JNI_INCLUDE_DIRS from command line: ${JNI_INCLUDE_DIRS}")
        include_directories(${JNI_INCLUDE_DIRS})
    elseif (JAVA_HOME)
        message(STATUS "Using JNI headers from JAVA_HOME: ${JAVA_HOME}")
        include_directories("${JAVA_HOME}/include" "${JAVA_HOME}/include/win32")
    elseif (DEFINED ENV{JAVA_HOME})
        message(STATUS "Using JNI headers from ENV{JAVA_HOME}: $ENV{JAVA_HOME}")
        include_directories("$ENV{JAVA_HOME}/include" "$ENV{JAVA_HOME}/include/win32")
    else ()
        message(FATAL_ERROR "JNI not found. Please provide JAVA_HOME or JNI_INCLUDE_DIRS")
    endif ()
endif ()

add_library(aboveabc80 SHARED
        src/main/cpp/native-lib.cpp
        src/main/cpp/sn76477.cpp
        src/main/cpp/Abc80Sound.cpp
        src/main/cpp/Patch.cpp
        src/main/cpp/Watchers.cpp
        src/main/cpp/z80.cpp
        src/main/cpp/z80main.cpp
        src/main/cpp/z80_ops.cpp
        src/main/cpp/run_zexall_test.cpp
        src/main/cpp/nomarch-1.4/nomarch.cpp
        src/main/cpp/nomarch-1.4/readrle.cpp
        src/main/cpp/nomarch-1.4/readhuff.cpp
        src/main/cpp/nomarch-1.4/readlzw.cpp
)

if (ANDROID)
    # Link standard Android NDK libraries
    target_link_libraries(aboveabc80 log jnigraphics)
    # Align LOAD segments at 16 KB boundaries for compatibility with 16 KB page devices (Android 15+)
    target_link_options(aboveabc80 PRIVATE "-Wl,-z,max-page-size=16384")
else ()
    # On desktop, no extra libraries needed
endif ()
