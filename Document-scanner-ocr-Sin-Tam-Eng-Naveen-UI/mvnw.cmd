@echo off
setlocal enabledelayedexpansion
set ROOT=%~dp0
set MAVEN_DIR=%ROOT%.mvn\apache-maven
if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
  echo Maven runtime not found in %MAVEN_DIR%.
  echo Downloading Apache Maven (~20-30MB)...
  set MAVEN_VERSION=3.9.6
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0.mvn\download_maven.ps1" "%~dp0" %MAVEN_VERSION%
  if errorlevel 1 (
    echo Failed to download or extract Maven. Please install Maven manually and ensure "mvn" is on PATH.
    exit /b 1
  )
)
"%MAVEN_DIR%\bin\mvn.cmd" %*
