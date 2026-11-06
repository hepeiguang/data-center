@echo off
rem Thin wrapper so the stack can be started by double-click.
rem Real logic lives in start-local.ps1 (single source of truth).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-local.ps1" %*
