@echo off
echo Building Release C++ Native Library and Packaging Windows MSI Installer...
call .\gradlew.bat :composeApp:packageMsi
if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: MSI Installer created at:
    for %%f in (composeApp\build\compose\binaries\main\msi\AboveABC80-*.msi) do echo %%f
) else (
    echo.
    echo BUILD FAILED.
)
