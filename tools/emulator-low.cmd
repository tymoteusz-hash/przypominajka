@echo off
start "" /b "%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone_API_37.0 -no-window -no-audio -no-boot-anim -memory 2048 -no-snapshot-save -gpu swiftshader_indirect
