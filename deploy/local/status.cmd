@echo off
rem Thin wrapper. Real logic lives in status.ps1.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0status.ps1" %*
