param(
    [switch]$WithNginx,
    [switch]$WithOpenMetadata,
    [int[]]$OnlyPorts,
    [switch]$DryRun,
    [string]$NginxExe = "D:\HPG\software\nginx-1.26.1\nginx-1.26.1\nginx.exe"
)

# ---------------------------------------------------------------------------
# Stops the local stack in reverse order. OpenMetadata and OpenSearch are left
# running unless -WithOpenMetadata is given: they take minutes to come back up.
# -OnlyPorts 12346 stops a single service, e.g. after rebuilding its jar.
# -DryRun only prints what would be stopped.
# ---------------------------------------------------------------------------

$ErrorActionPreference = "Continue"
$Run = Join-Path $PSScriptRoot "run"

function Stop-Proc([int]$ProcId, [string]$Label) {
    if ($DryRun) {
        Write-Host ("[dry ] would stop {0} (pid {1})" -f $Label, $ProcId)
        return
    }
    Stop-Process -Id $ProcId -Force -ErrorAction SilentlyContinue
}

if ($WithNginx -and -not $DryRun -and (Test-Path $NginxExe) -and (Test-Path "$Run\nginx.conf")) {
    Start-Process -FilePath $NginxExe -ArgumentList @("-s", "stop", "-c", "$Run\nginx.conf", "-p", "$Run") `
        -WindowStyle Hidden -Wait | Out-Null
    if (Test-Path "$Run\nginx.pid") {
        Stop-Proc ([int](Get-Content "$Run\nginx.pid" | Select-Object -First 1)) "nginx"
        Remove-Item "$Run\nginx.pid" -Force -ErrorAction SilentlyContinue
    }
    Start-Sleep -Seconds 1
}

$names = @("ds-ui", "data-asset", "data-api", "ds")
$ports = @(5173, 12347, 12346, 12345)
if ($WithOpenMetadata) {
    $names = @("openmetadata") + $names
    $ports = @(8585, 9200) + $ports
}
if ($OnlyPorts) {
    # only the listed ports, and only the pid file that matches one of them
    $ports = $OnlyPorts
    $byPort = @{ 5173 = "ds-ui"; 12347 = "data-asset"; 12346 = "data-api"; 12345 = "ds"; 8585 = "openmetadata"; 9200 = "opensearch" }
    $names = @($OnlyPorts | ForEach-Object { $byPort[[int]$_] } | Where-Object { $_ })
    Write-Host ("scoped to ports: {0}" -f ($ports -join ", "))
}

foreach ($name in $names) {
    $pidFile = Join-Path $Run "$name.pid"
    if (Test-Path $pidFile) {
        $procId = [int](Get-Content $pidFile | Select-Object -First 1)
        Stop-Proc $procId $name
        if (-not $DryRun) {
            Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
            Write-Host "stopped $name (pid $procId)"
        }
    }
}

# fallback: release the well-known ports if a pid file was lost
foreach ($port in $ports) {
    Get-NetTCPConnection -State Listen -LocalPort $port -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique |
        ForEach-Object {
            Stop-Proc $_ "port $port"
            if (-not $DryRun) { Write-Host "released port $port (pid $_)" }
        }
}

if ($WithOpenMetadata -and -not $DryRun) {
    $stopOs = "D:\HPG\workspace\LHYF\opensearch\stop-opensearch.cmd"
    if (Test-Path $stopOs) { cmd /c $stopOs | Out-Null }
}

Write-Host ""
& (Join-Path $PSScriptRoot "status.ps1")
