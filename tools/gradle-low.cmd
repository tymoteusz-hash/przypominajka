@echo off
set "JAVA_HOME=C:\Users\tymot\.jdks\jbr-21.0.11"
set "GRADLE=C:\Users\tymot\.gradle\wrapper\dists\gradle-8.11.1-bin\bpt9gzteqjrbo1mjrsomdt32c\gradle-8.11.1\bin\gradle.bat"
start "" /b /wait cmd /c ""%GRADLE%" -p "%~dp0.." %* --console=plain"
exit /b %ERRORLEVEL%
