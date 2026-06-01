@echo off
REM Gradle Wrapper script for Windows
REM This script can be used instead of "gradle" command

setlocal
set SCRIPT_DIR=%~dp0
cd /d %SCRIPT_DIR%

REM Execute gradle wrapper
call gradlew.bat %*
