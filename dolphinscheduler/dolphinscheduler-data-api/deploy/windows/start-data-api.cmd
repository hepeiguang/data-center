@echo off
REM Start one data-api instance. Usage: start-data-api.cmd 12346
setlocal
set PORT=%~1
if "%PORT%"=="" set PORT=12346

set JAVA_HOME=D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2
set APP_HOME=%~dp0..\..
set LOG_DIR=%APP_HOME%\logs

if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

REM cluster profile stores magic scripts in the shared metadata DB (magic_api_file)
"%JAVA_HOME%\bin\java" -Xms512m -Xmx2g ^
  -jar "%APP_HOME%\target\dolphinscheduler-data-api-3.4.3.jar" ^
  --spring.profiles.active=h2,cluster ^
  --server.port=%PORT% ^
  >> "%LOG_DIR%\data-api-%PORT%.log" 2>&1
