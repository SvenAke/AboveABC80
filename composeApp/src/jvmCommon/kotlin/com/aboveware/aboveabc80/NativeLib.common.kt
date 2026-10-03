package com.aboveware.aboveabc80

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

private fun loadNativeLibraryFromCopy(source: String, extension: String, input: InputStream): Boolean {
    try {
        input.use { stream ->
            val tempFile = File.createTempFile("abovecpm-", extension)
            tempFile.deleteOnExit()
            tempFile.outputStream().use { output -> stream.copyTo(output) }
            @Suppress("UnsafeDynamicallyLoadedCode")
            System.load(tempFile.absolutePath)
            System.err.println("NATIVE-LOAD: Successfully loaded a copy of $source from ${tempFile.absolutePath}")
        }
        return true
    } catch (e: IOException) {
        System.err.println("NATIVE-LOAD: Could not copy native library from $source: ${e.message}")
    } catch (e: UnsatisfiedLinkError) {
        System.err.println("NATIVE-LOAD: Could not load native library copied from $source: ${e.message}")
    } catch (e: SecurityException) {
        System.err.println("NATIVE-LOAD: Permission denied loading native library copied from $source: ${e.message}")
    }
    return false
}

actual fun loadNativeLibrary() {
    val userDir = System.getProperty("user.dir")
    System.err.println("NATIVE-LOAD: Working directory: $userDir")

    val osName = System.getProperty("os.name").lowercase()
    val isWindows = osName.contains("win")
    val libName = if (isWindows) "abovecpm.dll" else "libabovecpm.so"
    val extension = if (isWindows) ".dll" else ".so"

    val resourceStream = NativeLib::class.java.getResourceAsStream("/$libName")
    if (resourceStream != null) {
        if (loadNativeLibraryFromCopy("classpath resource $libName", extension, resourceStream)) {
            return
        }
    }

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
        if (file.isFile) {
            val loaded = try {
                loadNativeLibraryFromCopy(file.absolutePath, extension, FileInputStream(file))
            } catch (e: IOException) {
                System.err.println("NATIVE-LOAD: Could not open native library at ${file.absolutePath}: ${e.message}")
                false
            }
            if (loaded) {
                return
            }
        }
    }

    System.err.println("NATIVE-LOAD: FATAL - Could not load library from any location.")
    throw UnsatisfiedLinkError("Could not load native library 'abovecpm' from any of the searched locations. Checked paths: $paths")
}
