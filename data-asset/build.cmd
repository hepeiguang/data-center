@echo off
setlocal
set "JAVA_HOME=D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2"
set "MAVEN=D:\HPG\software\apache-maven-3.9.6\bin\mvn.cmd"
set "SETTINGS=D:\HPG\workspace\LHYF\tools\maven-settings-aliyun.xml"
cd /d "%~dp0"
call "%MAVEN%" -s "%SETTINGS%" %*
