@echo off
rem Thin wrapper. Add -WithOpenMetadata to also stop OpenMetadata and OpenSearch.
rem Real logic lives in stop-local.ps1.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-local.ps1" %*
