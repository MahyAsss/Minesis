@echo off
setlocal enabledelayedexpansion

if not exist "%GRADLE_USER_HOME%" (
    set GRADLE_USER_HOME=%USERPROFILE%\.gradle
)

set GRADLE_WRAPPER_DIR=%~dp0gradle\wrapper

if not exist "%GRADLE_USER_HOME%\wrapper\dists" (
    mkdir "%GRADLE_USER_HOME%\wrapper\dists"
)

echo Building Mimesis Mod...
echo.

REM Check if gradlew.bat wrapper exists, if not download Gradle
if not exist "%GRADLE_USER_HOME%\gradle-8.5\bin\gradle.bat" (
    echo Downloading Gradle 8.5...
    powershell -Command "& {Add-Type -AssemblyName 'System.Net.Http'; $client = New-Object System.Net.Http.HttpClient; $client.Timeout = [TimeSpan]::FromMinutes(10); $stream = $client.GetStreamAsync('https://services.gradle.org/distributions/gradle-8.5-bin.zip').Result; $file = [System.IO.File]::Create('%GRADLE_USER_HOME%\gradle-8.5.zip'); $stream.CopyTo($file); $file.Close()}"
    
    echo Extracting Gradle...
    powershell -Command "Add-Type -AssemblyName 'System.IO.Compression.FileSystem'; [System.IO.Compression.ZipFile]::ExtractToDirectory('%GRADLE_USER_HOME%\gradle-8.5.zip', '%GRADLE_USER_HOME%')"
)

set GRADLE_EXECUTABLE=%GRADLE_USER_HOME%\gradle-8.5\bin\gradle.bat

echo.
echo Running: gradle clean build -x test
echo.

call "%GRADLE_EXECUTABLE%" clean build -x test

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ============================================
    echo Build successful!
    echo JAR location: build\libs\mimesis-1.0.0.jar
    echo ============================================
) else (
    echo.
    echo Build failed with error code %ERRORLEVEL%
)

pause
