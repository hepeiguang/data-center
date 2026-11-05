param(
    [int]$OsPort = 9200,
    [int]$OmPort = 8585,
    [int]$DsPort = 12345,
    [int]$ApiPort = 12346,
    [int]$AssetPort = 12347,
    [int]$UiPort = 5173,
    [int]$GatewayPort = 0
)

# Port / pid / HTTP probe for every service of the local stack.

$services = @(
    @{ n = "OpenSearch";    p = $OsPort;    u = "http://127.0.0.1:$OsPort/" },
    @{ n = "OpenMetadata";  p = $OmPort;    u = "http://127.0.0.1:$OmPort/api/v1/system/version" },
    @{ n = "DS";            p = $DsPort;    u = "http://127.0.0.1:$DsPort/dolphinscheduler/actuator/health" },
    @{ n = "data-api";      p = $ApiPort;   u = "http://127.0.0.1:$ApiPort/data-api/health" },
    @{ n = "data-asset";    p = $AssetPort; u = "http://127.0.0.1:$AssetPort/data-asset/admin/health" },
    @{ n = "DS UI";         p = $UiPort;    u = "http://127.0.0.1:$UiPort/" }
)
if ($GatewayPort -gt 0) {
    $services += @{ n = "nginx"; p = $GatewayPort; u = "http://127.0.0.1:$GatewayPort/" }
}

Write-Host ("{0,-13} {1,-8} {2,-9} {3,-6} {4}" -f "service", "port", "pid", "http", "note")
Write-Host ("{0,-13} {1,-8} {2,-9} {3,-6} {4}" -f "-------", "----", "---", "----", "----")
foreach ($s in $services) {
    $owner = Get-NetTCPConnection -State Listen -LocalPort $s.p -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty OwningProcess
    if (-not $owner) {
        Write-Host ("{0,-13} :{1,-7} {2,-9} {3,-6} not listening" -f $s.n, $s.p, "-", "down")
        continue
    }
    $code = "-"
    $note = ""
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $s.u -TimeoutSec 8
        $code = [string]$response.StatusCode
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if ($status) { $code = [string]$status } else { $code = "ERR"; $note = "no response" }
    }
    if ($code -eq "200") {
        # vendor content types (e.g. the DS actuator) come back as byte[], not a string
        $body = if ($response.Content -is [byte[]]) {
            [System.Text.Encoding]::UTF8.GetString($response.Content)
        } else {
            [string]$response.Content
        }
        $note = ($body -replace "\s+", " ")
        if ($note.Length -gt 56) { $note = $note.Substring(0, 56) }
    }
    Write-Host ("{0,-13} :{1,-7} {2,-9} {3,-6} {4}" -f $s.n, $s.p, $owner, $code, $note)
}
