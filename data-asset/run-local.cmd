@echo off
setlocal
set "JAVA_HOME=D:\HPG\workspace\LHYF\tools\jdk-17"
cd /d "%~dp0"
if not exist "target\data-asset-service.jar" (
  echo target\data-asset-service.jar not found, run build.cmd first
  exit /b 1
)
echo Starting data-asset on http://127.0.0.1:12347/asset-console/
"%JAVA_HOME%\bin\java" -jar target\data-asset-service.jar
