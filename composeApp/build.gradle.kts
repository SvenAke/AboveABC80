import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.google.services)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }
    jvm()
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    sourceSets {
        val jvmCommon by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmCommon)
        jvmMain.get().dependsOn(jvmCommon)

        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(project.dependencies.platform(libs.firebase.bom))
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.core)
                implementation(kotlin("test"))
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.firebase.storage)
            implementation(libs.compose.material.icons.extended)
            
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutinesSwing)
                implementation(libs.compose.ui.graphics)
                implementation(libs.ktor.client.java)
            }
            resources.srcDir(layout.buildDirectory.dir("native/libs"))
        }
    }
}

val cmakeConfigure = tasks.register<Exec>("cmakeConfigure") {
    onlyIf { Os.isFamily(Os.FAMILY_WINDOWS) }
    val buildDir = layout.buildDirectory.dir("native/build").get().asFile
    val cmakeSourceDir = layout.projectDirectory.asFile.absolutePath
    
    inputs.file(layout.projectDirectory.file("CMakeLists.txt"))
    inputs.dir(layout.projectDirectory.dir("src/main/cpp"))
    outputs.upToDateWhen { false } // Force run to let CMake handle incrementality

    workingDir = buildDir
    
    val javaHome = System.getProperty("java.home").replace("\\", "/")
    val jniIncludeDir = "$javaHome/include"
    val jniPlatformIncludeDir = "$jniIncludeDir/win32"
    
    commandLine(
        "cmake", 
        "-DCMAKE_SH=OFF", 
        "-DCMAKE_BUILD_TYPE=Release",
        "-DJNI_INCLUDE_DIRS=$jniIncludeDir;$jniPlatformIncludeDir",
        cmakeSourceDir
    )
    
    doFirst {
        if (!buildDir.exists()) buildDir.mkdirs()
    }
}

val cmakeBuild = tasks.register<Exec>("cmakeBuild") {
    dependsOn(cmakeConfigure)
    onlyIf { Os.isFamily(Os.FAMILY_WINDOWS) }
    val buildDir = layout.buildDirectory.dir("native/build").get().asFile
    
    inputs.dir(layout.projectDirectory.dir("src/main/cpp"))
    outputs.upToDateWhen { false } // Force run

    workingDir = buildDir
    commandLine("cmake", "--build", ".", "--config", "Release")
}

val buildNativeDesktop = tasks.register("buildNativeDesktop") {
    dependsOn(cmakeBuild)
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    onlyIf { isWindows }
    val buildDir = layout.buildDirectory.dir("native/build")
    val outputDir = layout.buildDirectory.dir("native/libs")
    
    inputs.dir(layout.projectDirectory.dir("src/main/cpp"))
    outputs.dir(outputDir)
    
    doLast {
        val buildDirFile = buildDir.get().asFile
        val outputDirFile = outputDir.get().asFile
        outputDirFile.mkdirs()
        
        val searchDirs = listOf(
            buildDirFile,
            buildDirFile.resolve("Release"),
            buildDirFile.resolve("Debug"),
            buildDirFile.resolve("bin")
        )
        
        var found = false
        searchDirs.forEach { dir ->
            if (dir.exists()) {
                dir.listFiles()?.forEach { file ->
                    if (file.name == "aboveabc80.dll" || file.name == "libaboveabc80.so") {
                        val target = outputDirFile.resolve(file.name)
                        file.copyTo(target, overwrite = true)
                        println("NATIVE-BUILD: Copied ${file.name} from ${dir.name} to ${target.absolutePath}")
                        found = true
                    }
                }
            }
        }
        if (!found) {
            val allFiles = buildDirFile.walkTopDown().filter { it.name == "aboveabc80.dll" || it.name == "libaboveabc80.so" }.toList()
            allFiles.forEach { file ->
                val target = outputDirFile.resolve(file.name)
                file.copyTo(target, overwrite = true)
                println("NATIVE-BUILD: Found via walk and copied ${file.name} to ${target.absolutePath}")
                found = true
            }
        }
        if (!found) throw GradleException("NATIVE-BUILD: ERROR - No native library found in ${buildDirFile.absolutePath}")
    }
}

tasks.named("jvmProcessResources") {
    dependsOn(buildNativeDesktop)
}

tasks.withType<JavaExec>().configureEach {
    if (name.contains("run", ignoreCase = true)) {
        val libPath = layout.buildDirectory.dir("native/libs").get().asFile.absolutePath
        systemProperty("java.library.path", libPath)
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

android {
    namespace = "com.aboveware.aboveabc80"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aboveware.aboveabc80"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.aboveware.aboveabc80.MainKt"
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "AboveABC80"
            packageVersion = "1.0.0"
            
            // Låter jpackage hitta sidobilder (main-dialog.bmp, main-banner.bmp) i denna mapp
            appResourcesRootDir.set(project.layout.projectDirectory.dir("package"))

            windows {
                iconFile.set(project.file("src/commonMain/composeResources/drawable/windows_icon.ico"))
                shortcut = true
                menu = true
                // A unique UUID for the installer to handle upgrades
                upgradeUuid = "6f52e582-7d2d-4566-a34f-0363228c232f"
            }
        }
    }
}
