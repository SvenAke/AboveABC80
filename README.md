This is a Kotlin Multiplatform project targeting Android, Desktop (JVM).

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

---

### Cassette cloud downloads

Downloading files from the cloud in the cassette dialog saves all selected files
to the app's internal saved-tapes storage without importing them into the current
tape. Use **Saved tapes** to open and import a downloaded file explicitly.

### Cassette activity

BASIC tape reads and writes display an animated cassette over a dimmed emulator
screen. The red REC indicator lights up while writing; reading offers a Cancel
button. The animation disappears when the tape motor stops. Downloading or
importing a cassette does not start this animation.
Temporary PIO interrupt changes between LOAD blocks do not switch the cassette
to recording mode or restart the animation.
While searching for a named program, each non-matching program name is displayed
with a one-second search pause. Cancel interrupts this pause promptly.
If the tape ends before the requested program is found, a dialog offers to
rewind and retry the same LOAD from the beginning, or cancel the search.

To load a program with DOS enabled, rewind the inserted tape and use
`LOAD CAS:MUSIK.BAS`, then `RUN`. The `CAS:` prefix selects the cassette rather
than the default disk device.

The desktop cassette regression test boots the BASIC ROM, loads a multi-block
program after another file on a synthetic tape at normal CPU speed, and verifies
the result with `LIST`. Run it with
`.\gradlew.bat :composeApp:jvmTest --tests com.aboveware.aboveabc80.CassetteLoadIntegrationTest`.
It uses temporary storage instead of the user's saved tapes.

### ABC80 character graphics

The screen renderer honors the in-row graphics control `CHR$(23)` and text
control `CHR$(22)`. In graphics mode, mosaic characters form continuous 2-by-3
pixel blocks, while letters in the middle character range remain text. Each row
starts in text mode. This allows programs such as MUSIK.BAS to draw their keyboard
without changing the source-code display in `LIST`.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…