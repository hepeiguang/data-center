@echo off
setlocal
if "%JAVA_HOME%"=="" set JAVA_HOME=D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2
if "%DS_HOME%"=="" set DS_HOME=D:\HPG\workspace\LHYF\apache-dolphinscheduler-3.4.3-bin
set PATCH_HOME=%~dp0
set LIB=%DS_HOME%\api-server\libs
set OUT=%PATCH_HOME%classes
set SRC=%PATCH_HOME%src\org\apache\dolphinscheduler\plugin\storage\api\AbstractStorageOperator.java

if not exist "%LIB%\dolphinscheduler-storage-api-3.4.3.jar" (
  echo DS_HOME libs not found: %LIB%
  exit /b 1
)

for %%F in ("%LIB%\commons-lang3-*.jar") do set LANG3=%%~fF
for %%F in ("%LIB%\guava-*.jar") do set GUAVA=%%~fF

set CP=%LIB%\dolphinscheduler-storage-api-3.4.3.jar;%LIB%\dolphinscheduler-spi-3.4.3.jar;%LIB%\dolphinscheduler-common-3.4.3.jar;%LANG3%;%GUAVA%

if exist "%OUT%" rmdir /s /q "%OUT%"
mkdir "%OUT%"
"%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -cp "%CP%" -d "%OUT%" "%SRC%"
if errorlevel 1 exit /b 1

for %%D in (api-server master-server worker-server alert-server tools) do (
  if exist "%DS_HOME%\%%D\libs\dolphinscheduler-storage-api-3.4.3.jar" (
    echo Updating %%D\libs\dolphinscheduler-storage-api-3.4.3.jar
    "%JAVA_HOME%\bin\jar.exe" uf "%DS_HOME%\%%D\libs\dolphinscheduler-storage-api-3.4.3.jar" -C "%OUT%" .
  )
)
if exist "%DS_HOME%\libs\dolphinscheduler-storage-api-3.4.3.jar" (
  echo Updating libs\dolphinscheduler-storage-api-3.4.3.jar
  "%JAVA_HOME%\bin\jar.exe" uf "%DS_HOME%\libs\dolphinscheduler-storage-api-3.4.3.jar" -C "%OUT%" .
)
echo Done. Restart DolphinScheduler standalone.
endlocal
