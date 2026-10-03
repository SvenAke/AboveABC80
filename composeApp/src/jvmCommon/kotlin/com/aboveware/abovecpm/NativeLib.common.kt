package com.aboveware.abovecpm

import java.io.File

actual fun loadNativeLibrary() {
    val userDir = System.getProperty("user.dir")
    System.err.println("NATIVE-LOAD: Working directory: $userDir")

    // 1. Try standard loadLibrary first
    try {
        System.loadLibrary("abovecpm")
        System.err.println("NATIVE-LOAD: Successfully loaded 'abovecpm' via loadLibrary")
        return
    } catch (e: UnsatisfiedLinkError) {
        System.err.println("NATIVE-LOAD: loadLibrary('abovecpm') failed: ${e.message}")
    }

    val osName = System.getProperty("os.name").lowercase()
    val isWindows = osName.contains("win")
    val libName = if (isWindows) "abovecpm.dll" else "libabovecpm.so"

    // 2. Try to load from classpath/resources
    val resourceStream = NativeLib::class.java.getResourceAsStream("/$libName")
    if (resourceStream != null) {
        try {
            val tempFile = File.createTempFile("abovecpm-", if (isWindows) ".dll" else ".so")
            tempFile.deleteOnExit()
            resourceStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            @Suppress("UnsafeDynamicallyLoadedCode")
            System.load(tempFile.absolutePath)
            System.err.println("NATIVE-LOAD: Successfully loaded from resources via temp file ${tempFile.absolutePath}")
            return
        } catch (e: Exception) {
            System.err.println("NATIVE-LOAD: Failed to load from resources: ${e.message}")
        }
    }

    // 3. List potential paths relative to the working directory
    val paths = listOf(
        "build/native/libs/$libName",
        "composeApp/build/native/libs/$libName",
        "../composeApp/build/native/libs/$libName",
        "./native/libs/$libName",
        "composeApp/build/native/build/Release/$libName",
        "composeApp/build/native/build/$libName",
        "build/native/build/Release/$libName"
    )

    for (path in paths) {
        val file = File(path)
        System.err.println("NATIVE-LOAD: Checking path: ${file.absolutePath}")
        if (file.exists()) {
            try {
                @Suppress("UnsafeDynamicallyLoadedCode")
                System.load(file.absolutePath)
                System.err.println("NATIVE-LOAD: Successfully loaded from ${file.absolutePath}")
                return
            } catch (e: UnsatisfiedLinkError) {
                System.err.println("NATIVE-LOAD: Found file at ${file.absolutePath} but load failed: ${e.message}")
            }
        }
    }

    System.err.println("NATIVE-LOAD: FATAL - Could not load library from any location.")
    throw UnsatisfiedLinkError("Could not load native library 'abovecpm' from any of the searched locations. Checked paths: $paths")
}
