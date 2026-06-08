@echo off
setlocal enabledelayedexpansion
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "c:\Users\Felix\Downloads\DemoCorrigida\Demo_Mongo_teste"
echo Starting tests...
"C:\Users\Felix\.maven\maven-3.9.14\bin\mvn.cmd" test -q
echo Exit code: %ERRORLEVEL%
if %ERRORLEVEL% == 0 (echo SUCCESS) else (echo FAILURE)
