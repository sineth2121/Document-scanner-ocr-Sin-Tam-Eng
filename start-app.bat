@echo off
echo Starting Document Scanner Application...
cd "%~dp0Document-scanner-ocr-Sin-Tam-Eng-Naveen-UI"
set MAVEN_OPTS=-Xmx512m
call mvnw.cmd clean javafx:run
pause
