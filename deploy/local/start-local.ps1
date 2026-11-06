param(
    [string]$JavaHome = "D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2",
    [string]$LhyfRoot = "D:\HPG\workspace\LHYF",
    [string]$DsHome = "D:\HPG\workspace\LHYF\apache-dolphinscheduler-3.4.3-bin",
    [string]$StHome = "D:\HPG\workspace\LHYF\apache-seatunnel-3.0.0",
    [string]$DataApiJar = "",
    [string]$AssetJar = "",
    [string]$UiHome = "",
    [string]$DataDir = "D:\HPG\workspace\LHYF\data",
    [int]$OsPort = 9200,
    [int]$OmPort = 8585,
    [int]$DsPort = 12345,
    [int]$ApiPort = 12346,
    [int]$AssetPort = 12347,
    [int]$UiPort = 5173,
    # all   = six services; api = DS + data-api + UI (no OM/OpenSearch);
    # asset = DS + data-asset + OM + OpenSearch + UI (no data-api).
    # Default is api: local dev no longer needs OpenMetadata/OpenSearch running,
    # and that pair alone is ~1.4GB RSS.
    [ValidateSet("all", "api", "asset")]
    [string]$Profile = "api",
    # sized from jstat -gc live sets measured on this box (see README "内存预算")
    [string]$DsJvmOpts = "-Xms256m -Xmx512m -Xss512k -XX:MaxMetaspaceSize=256m",
    [string]$ApiJvmOpts = "-Xms128m -Xmx256m -Xss512k -XX:MaxMetaspaceSize=192m",
    [string]$AssetJvmOpts = "-Xms64m -Xmx256m -Xss512k -XX:MaxMetaspaceSize=192m",
    [string]$OmJvmOpts = "-Xms512m -Xmx1024m -Xss512k -XX:MaxMetaspaceSize=320m",
    [string]$OsJvmOpts = "-Xms512m -Xmx640m -Xss512k -XX:MaxMetaspaceSize=256m",
    # the index normally runs on the 2C2G server (see env.sh ELASTICSEARCH_HOST);
    # pass -WithLocalIndex to start a local OpenSearch instead
    [switch]$WithLocalIndex,
    [switch]$Force,
    [switch]$WithNginx,
    [string]$NginxExe = "D:\HPG\software\nginx-1.26.1\nginx-1.26.1\nginx.exe"
)

# ---------------------------------------------------------------------------
# One command for the local stack, in dependency order:
#   [OpenSearch :9200] -> OpenMetadata :8585 -> DS standalone :12345
#   -> data-api :12346 -> data-asset :12347 -> DS UI dev :5173 (-> nginx :8088)
# The search index normally runs on the 2C2G server (openmetadata/env.sh points at
# http://123.57.2.64:9200), so OpenSearch is NOT started here by default; pass
# -WithLocalIndex if you want the old local one. Services whose port is already
# listening are skipped, so re-running this script tops up a partial stack.
# -Force turns an occupied port into an error. -Profile picks which services are
# needed so a 2C-class dev box does not have to hold the whole stack.
# Default is -Profile api (DS + data-api + UI); pass -Profile asset when the
# catalog/lineage/governance pages need OpenMetadata, or -Profile all for both.
# DS and the UI are always started: the UI requires a DS login, and data-api
# shares DS's H2 metadata file, so they cannot live on different hosts.
# ---------------------------------------------------------------------------

$ErrorActionPreference = "Stop"
$Root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
if (-not $DataApiJar) {
    $DataApiJar = Join-Path $Root "dolphinscheduler\dolphinscheduler-data-api\target\dolphinscheduler-data-api-3.4.3.jar"
}
if (-not $AssetJar) {
    $AssetJar = Join-Path $Root "data-asset\target\data-asset-service.jar"
}
if (-not $UiHome) {
    $UiHome = Join-Path $Root "dolphinscheduler\dolphinscheduler-ui"
}
$Run = Join-Path $PSScriptRoot "run"
New-Item -ItemType Directory -Path $Run -Force | Out-Null

$WantIndex = $Profile -in @("all", "asset")   # OpenMetadata (its index lives on the server)
$WantLocalOs = $WithLocalIndex                # local OpenSearch is opt-in
$WantApi = $Profile -in @("all", "api")       # data-api (magic console)
$WantAsset = $Profile -in @("all", "asset")   # data-asset

function Get-PortOwner([int]$Port) {
    Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty OwningProcess
}

function Wait-Port([int]$Port, [int]$TimeoutSec = 180) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $client = New-Object Net.Sockets.TcpClient
            $client.Connect("127.0.0.1", $Port)
            $client.Close()
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Ensure-Service([string]$Name, [int]$Port, [scriptblock]$Start, [int]$TimeoutSec, [string]$LogHint) {
    $owner = Get-PortOwner $Port
    if ($owner) {
        if ($Force) {
            throw "$Name port $Port is already in use (pid $owner); run .\stop-local.ps1 first"
        }
        Write-Host ("[skip] {0,-14} :{1} already running (pid {2})" -f $Name, $Port, $owner)
        return
    }
    Write-Host ("[start] {0} ..." -f $Name)
    & $Start
    if (-not (Wait-Port $Port $TimeoutSec)) {
        throw ("{0} did not open port {1} within {2}s; see {3}" -f $Name, $Port, $TimeoutSec, $LogHint)
    }
    Write-Host ("[ ok ] {0,-14} :{1}" -f $Name, $Port)
}

foreach ($p in @("$JavaHome\bin\java.exe", "$DsHome\standalone-server\libs", "$StHome\bin\seatunnel.sh", $DataApiJar)) {
    if (-not (Test-Path $p)) { throw "missing prerequisite: $p" }
}

$env:JAVA_HOME = $JavaHome
$env:SEATUNNEL_HOME = $StHome
$GitBin = "D:\HPG\software\Git"
$env:PATH = "$JavaHome\bin;$StHome\bin;$GitBin\bin;$GitBin\usr\bin;$env:PATH"
$HadoopHome = Join-Path $LhyfRoot "tools\hadoop-3.1.1"
if (Test-Path $HadoopHome) {
    $env:HADOOP_HOME = $HadoopHome
    $env:PATH = "$HadoopHome\bin;$env:PATH"
}

$dbFile = Join-Path $DataDir "dsdb.mv.db"
$dbUrl = "jdbc:h2:file:$($DataDir.Replace('\','/'))/dsdb;MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true"

if ($WantLocalOs) {
    Ensure-Service "OpenSearch" $OsPort {
        # launch opensearch.bat directly: the .cmd wrapper uses `start`, which dies when this
        # process already owns redirected stdout/stderr handles
        $osHome = Join-Path $LhyfRoot "opensearch\opensearch-3.4.0"
        if (-not (Test-Path "$osHome\bin\opensearch.bat")) { throw "missing $osHome\bin\opensearch.bat" }
        if (-not (Test-Path "$osHome\jdk\bin\java.exe")) { throw "missing bundled JDK: $osHome\jdk" }
        # OpenSearch needs Java 21; the stack default JAVA_HOME is JDK 11, so scope the override.
        # OPENSEARCH_JAVA_OPTS is appended after config/jvm.options, so it wins over the -Xmx there.
        $previousJavaHome = $env:JAVA_HOME
        try {
            $env:JAVA_HOME = "$osHome\jdk"
            $env:OPENSEARCH_JAVA_OPTS = $OsJvmOpts
            $os = Start-Process -FilePath "$osHome\bin\opensearch.bat" -WorkingDirectory $osHome `
                -RedirectStandardOutput "$Run\opensearch.out.log" -RedirectStandardError "$Run\opensearch.err.log" `
                -WindowStyle Hidden -PassThru
            Set-Content -Path "$Run\opensearch.pid" -Value $os.Id
        } finally {
            $env:JAVA_HOME = $previousJavaHome
            Remove-Item Env:OPENSEARCH_JAVA_OPTS -ErrorAction SilentlyContinue
        }
    } 300 "$Run\opensearch.out.log"
}

if ($WantIndex) {
    Ensure-Service "OpenMetadata" $OmPort {
        $launch = Join-Path $LhyfRoot "openmetadata\start-om.cmd"
        if (-not (Test-Path $launch)) { throw "missing $launch" }
        # env.sh honours a pre-set OM_JAVA_OPTS; clear it right after so it cannot leak
        try {
            $env:OM_JAVA_OPTS = $OmJvmOpts
            Start-Process -FilePath "cmd.exe" -ArgumentList @("/c", $launch) -WindowStyle Hidden `
                -RedirectStandardOutput "$Run\openmetadata.out.log" -RedirectStandardError "$Run\openmetadata.err.log" | Out-Null
        } finally {
            Remove-Item Env:OM_JAVA_OPTS -ErrorAction SilentlyContinue
        }
    } 360 "$Run\openmetadata.out.log (and openmetadata\logs\openmetadata.err)"
}

Ensure-Service "DS" $DsPort {
    $cp = @(
        "$DsHome\standalone-server\conf",
        "$DsHome\api-server\libs\*",
        "$DsHome\master-server\libs\*",
        "$DsHome\worker-server\libs\*",
        "$DsHome\alert-server\libs\*",
        "$DsHome\plugins\alert-plugins\*",
        "$DsHome\plugins\datasource-plugins\*",
        "$DsHome\plugins\storage-plugins\*",
        "$DsHome\plugins\task-plugins\*",
        "$DsHome\standalone-server\libs\*"
    ) -join ";"
    $dsArgs = @(
        "-server", "-XX:+IgnoreUnrecognizedVMOptions", "-Duser.timezone=Asia/Shanghai"
    ) + $DsJvmOpts.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries) + @(
        "-cp", $cp, "org.apache.dolphinscheduler.StandaloneServer",
        "--spring.datasource.url=$dbUrl",
        "--registry.hikari-config.jdbc-url=$dbUrl"
    )
    if (-not (Test-Path $dbFile)) {
        Write-Host "fresh metadata DB, initializing schema..."
        $dsArgs += "--spring.sql.init.mode=always"
    }
    $ds = Start-Process -FilePath "$JavaHome\bin\java.exe" -ArgumentList $dsArgs `
        -WorkingDirectory "$DsHome\standalone-server" `
        -RedirectStandardOutput "$Run\ds.out.log" -RedirectStandardError "$Run\ds.err.log" `
        -WindowStyle Hidden -PassThru
    Set-Content -Path "$Run\ds.pid" -Value $ds.Id
} 300 "$Run\ds.out.log"

if ($WantApi) {
    Ensure-Service "data-api" $ApiPort {
        $env:SPRING_DATASOURCE_URL = $dbUrl
        try {
            $api = Start-Process -FilePath "$JavaHome\bin\java.exe" `
                -ArgumentList (@($ApiJvmOpts.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)) +
                    @("-jar", $DataApiJar, "--spring.profiles.active=h2", "--server.port=$ApiPort")) `
                -WorkingDirectory (Join-Path $Root "dolphinscheduler\dolphinscheduler-data-api") `
                -RedirectStandardOutput "$Run\data-api.out.log" -RedirectStandardError "$Run\data-api.err.log" `
                -WindowStyle Hidden -PassThru
            Set-Content -Path "$Run\data-api.pid" -Value $api.Id
        } finally {
            # SPRING_DATASOURCE_URL is inherited by every child process: clear it before data-asset starts
            Remove-Item Env:SPRING_DATASOURCE_URL -ErrorAction SilentlyContinue
        }
    } 180 "$Run\data-api.out.log"
}

if ($WantAsset) {
    Ensure-Service "data-asset" $AssetPort {
        if (-not (Test-Path $AssetJar)) {
            throw "missing $AssetJar - build it first: cmd /c data-asset\build.cmd -DskipTests package"
        }
        # absolute config-DB path: the default is relative to the process cwd and would move with it
        $assetHome = Join-Path $Root "data-asset"
        $assetDb = Join-Path $assetHome "data\data-asset"
        $asset = Start-Process -FilePath "$JavaHome\bin\java.exe" `
            -ArgumentList (@($AssetJvmOpts.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries)) +
                @("-jar", $AssetJar, "--server.port=$AssetPort",
                    "--spring.datasource.url=jdbc:h2:file:$($assetDb.Replace('\','/'));MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")) `
            -WorkingDirectory $assetHome `
            -RedirectStandardOutput "$Run\data-asset.out.log" -RedirectStandardError "$Run\data-asset.err.log" `
            -WindowStyle Hidden -PassThru
        Set-Content -Path "$Run\data-asset.pid" -Value $asset.Id
    } 180 "$Run\data-asset.out.log"
}

Ensure-Service "DS UI" $UiPort {
    if (-not (Test-Path (Join-Path $UiHome "node_modules"))) {
        throw "missing $UiHome\node_modules - run npm install in dolphinscheduler-ui first"
    }
    $ui = Start-Process -FilePath "cmd.exe" `
        -ArgumentList @("/c", "npm run dev -- --port $UiPort") -WorkingDirectory $UiHome `
        -RedirectStandardOutput "$Run\ds-ui.out.log" -RedirectStandardError "$Run\ds-ui.err.log" `
        -WindowStyle Hidden -PassThru
    Set-Content -Path "$Run\ds-ui.pid" -Value $ui.Id
} 120 "$Run\ds-ui.out.log"

if ($WantApi) {
    try {
        $health = Invoke-RestMethod -Uri "http://127.0.0.1:$ApiPort/data-api/health" -TimeoutSec 10
        if ($health.status -ne "UP") { throw "status=$($health.status)" }
    } catch {
        throw "data-api health check failed: $($_.Exception.Message); see $Run\data-api.out.log"
    }
}

if ($WithNginx) {
    if (-not (Test-Path $NginxExe)) { throw "nginx not found: $NginxExe" }
    $nginxConf = @"
daemon on;
worker_processes 1;
pid logs/nginx.pid;
error_log logs/nginx-error.log warn;
events { worker_connections 1024; }
http {
    client_body_temp_path temp/client_body_temp;
    proxy_temp_path temp/proxy_temp;
    fastcgi_temp_path temp/fastcgi_temp;
    uwsgi_temp_path temp/uwsgi_temp;
    scgi_temp_path temp/scgi_temp;
    access_log logs/nginx-access.log;
    client_max_body_size 1024m;
    proxy_http_version 1.1;
    proxy_set_header Host `$host;
    proxy_set_header X-Real-IP `$remote_addr;
    proxy_set_header X-Forwarded-For `$proxy_add_x_forwarded_for;
    map `$http_upgrade `$connection_upgrade { default upgrade; '' close; }
    server {
        listen 8088;
        location = / { return 302 /dolphinscheduler/ui/; }
        location /dolphinscheduler/ {
            proxy_pass http://127.0.0.1:$DsPort;
            proxy_set_header Upgrade `$http_upgrade;
            proxy_set_header Connection `$connection_upgrade;
        }
        location /data-api/ { proxy_pass http://127.0.0.1:$ApiPort; }
        location /magic/ {
            proxy_pass http://127.0.0.1:$ApiPort;
            proxy_set_header Upgrade `$http_upgrade;
            proxy_set_header Connection `$connection_upgrade;
        }
        location /data-asset/ { proxy_pass http://127.0.0.1:$AssetPort; }
    }
}
"@
    New-Item -ItemType Directory -Path "$Run\logs" -Force | Out-Null
    foreach ($d in @("client_body_temp", "proxy_temp", "fastcgi_temp", "uwsgi_temp", "scgi_temp")) {
        New-Item -ItemType Directory -Path "$Run\temp\$d" -Force | Out-Null
    }
    # UTF8Encoding($false) avoids the BOM that nginx rejects on Windows PowerShell 5.1
    [System.IO.File]::WriteAllText("$Run\nginx.conf", $nginxConf, (New-Object System.Text.UTF8Encoding($false)))
    Ensure-Service "nginx" 8088 {
        # nginx.exe stays in the foreground on Windows; Start-Process detaches it
        $nginx = Start-Process -FilePath $NginxExe -ArgumentList @("-c", "$Run\nginx.conf", "-p", "$Run") `
            -WindowStyle Hidden -PassThru
        Set-Content -Path "$Run\nginx.pid" -Value $nginx.Id
    } 30 "$Run\logs\nginx-error.log"
}

Write-Host ""
& (Join-Path $PSScriptRoot "status.ps1")
Write-Host ""
Write-Host ("UI        : http://127.0.0.1:{0}/          (admin / dolphinscheduler123)" -f $UiPort)
Write-Host ("DS console: http://127.0.0.1:{0}/dolphinscheduler/ui/" -f $DsPort)
Write-Host ("profile   : {0}" -f $Profile)
if ($WantApi) { Write-Host ("magic-api : http://127.0.0.1:{0}/magic/web" -f $ApiPort) }
if ($WantIndex) {
    Write-Host ("OM console: http://127.0.0.1:{0}/" -f $OmPort)
    Write-Host ("asset pages : http://127.0.0.1:{0}/asset/catalog  /asset/lineage  /asset/governance" -f $UiPort)
}
if ($WithNginx) { Write-Host "gateway   : http://127.0.0.1:8088/" }
Write-Host ("logs      : {0}" -f $Run)
Write-Host ("stop      : .\stop-local.ps1{0}" -f $(if ($WithNginx) { " -WithNginx" } else { "" }))
