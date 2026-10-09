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

### Startup

Startup prepares character sets and restores saved disks on background
dispatchers so file I/O and disk parsing do not block Android input handling.

### ABC80 character glyphs

ABC80 display and terminal glyphs share the original `Abc80MonitorCharacterMap`
8-by-14 pixel definitions, including Swedish characters and cassette graphics.
The terminal does not require character ROM loading to render these glyphs.

### ADM-3A keyboard

The Upper Case key shows the red pressed-key background while held. Each press
toggles its red LED; releasing the key leaves the LED state unchanged. This key
does not send Escape to the emulator.

### Cassette cloud downloads

Downloading files from the cloud in the cassette dialog saves all selected files
to the app's internal saved-tapes storage without importing them into the current
tape. Use **Saved tapes** to open and import a downloaded file explicitly.

Cloud cassette WAV files are streamed directly to internal storage with a
64 KiB buffer, rather than loaded into memory or cached in the diskette importer.
This also applies when browsing `abc80/cassettes/` from the general cloud dialog.
The dialog shows downloaded bytes and reports HTTP/storage errors. Incomplete
downloads are removed; an existing saved tape is replaced only after the full
download has been checked.

Opening a WAV recording from **Saved tapes** also streams the file directly
through the cassette decoder on a background thread. Only decoded cassette
blocks are retained in memory, not the full WAV recording.

### Cassette activity

The cassette button in the upper-right corner, between the disk indicator and
FPS, opens the cassette dialog. The icon-only button remains visible when CPU/FPS
is hidden. A circular progress indicator is shown while the selected tape is
imported and its files are listed; this work runs off the UI thread.
The cassette and disk manager dialogs' icon buttons show localized tooltips on
hover or long press.

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

### ABC80 sound

Key presses do not play click sounds. Android keyboard haptic feedback and
program-generated sound remain available.

Settings includes a saved **Sound volume** slider (0-400 %) and a mute button.
100 % is the normal level; higher values boost quiet programs such as MUSIK.BAS.
Changes apply immediately on desktop and Android. The output removes DC bias
before amplification, smoothly changes gain, and limits PCM to the valid range.
Boosting loud sounds can cause clipping. Hold a musical key to sustain its note.

The native SN76477 emulator receives writes to ABC80 sound port 6 and generates
44.1 kHz mono PCM timed to the CPU cycles. Desktop uses Java Sound; Android uses
AudioTrack. The BASIC ROM bell (`PRINT CHR$(7)`) and `OUT 6,value` use the same
chip, including its oscillator, noise, mixer and envelope controls.

Short software-generated sound pulses (including MUSIK.BAS) are integrated at
16 times the output sample rate before downsampling. The envelope capacitor
keeps its charge across short inhibit pulses instead of being reset instantly.
Physical and on-screen keys remain pressed until release, so MUSIK plays a
note for as long as its key is held. Pasted text retains timed key pulses.

For example, `OUT 6,0:OUT 6,3` starts a tone and `OUT 6,0` disables sound.
Audio is muted while the CPU is frozen or running at MAX speed. A bounded
100 ms buffer prevents old sound from accumulating when playback falls behind.
Audio-device failures are logged and displayed in the app.

The BASIC-ROM regression test above also checks tone, noise, bell, normalized
PCM, disabling sound, freeze/MAX muting, and MUSIK's machine-code routine
(sustained notes, pitch changes, and key release) without requiring an audio
device. Set `ABC80_MUSIC_TAPE` to a raw cassette image to additionally test
the complete MUSIK.BAS program from that image; the source image is not modified.

### ABC80 character graphics

TKN80 supports an 80-by-24 screen. Settings includes **Start with TKN80 (80
columns)**, applied on the next app start (40 columns by default). BASIC can switch
at runtime with `PRINT INP(4)` for 80 columns or `PRINT INP(3)` for 40 columns.
The modes have separate screen RAM; clear-screen clears both buffers.

The screen renderer honors the in-row graphics control `CHR$(23)` and text
control `CHR$(22)`. In graphics mode, mosaic characters form continuous 2-by-3
pixel blocks, while letters in the middle character range remain text. Each row
starts in text mode. This allows programs such as MUSIK.BAS to draw their keyboard
without changing the source-code display in `LIST`.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…