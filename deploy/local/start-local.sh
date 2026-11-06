#!/usr/bin/env bash
# Single-host deployment for Linux: DS standalone + data-api + data-asset.
# DS and data-api share one H2 metadata file, so they must stay on the same host.
set -euo pipefail

JAVA_HOME="${JAVA_HOME:-/opt/jdk-11}"
# data-asset is Spring Boot 3.0 (JDK 17+); data-api moved to Boot 3.0.13/JDK 17 in the boot3 console migration
ASSET_JAVA_HOME="${ASSET_JAVA_HOME:-/opt/jdk-17}"
API_JAVA_HOME="${API_JAVA_HOME:-$ASSET_JAVA_HOME}"
DS_HOME="${DS_HOME:-/opt/apache-dolphinscheduler-3.4.3-bin}"
ST_HOME="${ST_HOME:-/opt/apache-seatunnel-3.0.0}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DATA_API_JAR="${DATA_API_JAR:-$ROOT/dolphinscheduler/dolphinscheduler-data-api/target/dolphinscheduler-data-api-3.4.3.jar}"
DATA_DIR="${DATA_DIR:-/data}"
DS_PORT="${DS_PORT:-12345}"
API_PORT="${API_PORT:-12346}"
ASSET_PORT="${ASSET_PORT:-12347}"
ASSET_JAR="${ASSET_JAR:-$ROOT/data-asset/target/data-asset-service.jar}"

RUN="$ROOT/deploy/local/run"
mkdir -p "$RUN" "$DATA_DIR"

# Heap caps sized from jstat -gc live sets (DS old ~264MB, data-api ~41MB, data-asset ~12MB).
# Override per host with DS_OPTS / API_OPTS / ASSET_OPTS.
DS_OPTS="${DS_OPTS:--Xms256m -Xmx512m -Xss512k -XX:MaxMetaspaceSize=256m}"
API_OPTS="${API_OPTS:--Xms128m -Xmx256m -Xss512k -XX:MaxMetaspaceSize=192m}"
ASSET_OPTS="${ASSET_OPTS:--Xms64m -Xmx256m -Xss512k -XX:MaxMetaspaceSize=192m}"

for p in "$JAVA_HOME/bin/java" "$ASSET_JAVA_HOME/bin/java" "$API_JAVA_HOME/bin/java" "$DS_HOME/standalone-server/libs" "$ST_HOME/bin/seatunnel.sh" "$DATA_API_JAR"; do
    [ -e "$p" ] || { echo "missing prerequisite: $p" >&2; exit 1; }
done

for port in "$DS_PORT" "$API_PORT" "$ASSET_PORT"; do
    if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
        exec 3>&- 3<&- || true
        echo "port $port is already in use; run stop-local.sh first" >&2
        exit 1
    fi
done

export JAVA_HOME SEATUNNEL_HOME="$ST_HOME"
export PATH="$JAVA_HOME/bin:$ST_HOME/bin:$PATH"

wait_port() {
    local port="$1" timeout="${2:-180}" i=0
    while [ "$i" -lt "$timeout" ]; do
        if (exec 3<>"/dev/tcp/127.0.0.1/$port") 2>/dev/null; then
            exec 3>&- 3<&- || true
            return 0
        fi
        sleep 2
        i=$((i + 2))
    done
    return 1
}

DB_URL="jdbc:h2:file:$DATA_DIR/dsdb;MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true"
CP="$DS_HOME/standalone-server/conf"
CP="$CP:$DS_HOME/api-server/libs/*"
CP="$CP:$DS_HOME/master-server/libs/*"
CP="$CP:$DS_HOME/worker-server/libs/*"
CP="$CP:$DS_HOME/alert-server/libs/*"
CP="$CP:$DS_HOME/plugins/alert-plugins/*"
CP="$CP:$DS_HOME/plugins/datasource-plugins/*"
CP="$CP:$DS_HOME/plugins/storage-plugins/*"
CP="$CP:$DS_HOME/plugins/task-plugins/*"
CP="$CP:$DS_HOME/standalone-server/libs/*"

INIT_ARG=""
[ -f "$DATA_DIR/dsdb.mv.db" ] || INIT_ARG="--spring.sql.init.mode=always"

echo "starting DolphinScheduler standalone on $DS_PORT ..."
cd "$DS_HOME/standalone-server"
nohup "$JAVA_HOME/bin/java" -server $DS_OPTS \
    -XX:+IgnoreUnrecognizedVMOptions -Duser.timezone="${TZ:-Asia/Shanghai}" \
    -cp "$CP" org.apache.dolphinscheduler.StandaloneServer \
    --spring.datasource.url="$DB_URL" \
    --registry.hikari-config.jdbc-url="$DB_URL" $INIT_ARG \
    > "$RUN/ds.out.log" 2>&1 &
echo $! > "$RUN/ds.pid"

wait_port "$DS_PORT" 240 || { echo "DS failed to start, see $RUN/ds.out.log" >&2; exit 1; }

echo "starting data-api on $API_PORT ..."
DATA_API_HOME="${DATA_API_HOME:-$(cd "$(dirname "$DATA_API_JAR")/.." && pwd)}"
cd "$DATA_API_HOME"
# only data-api uses the DS H2 file; unset before data-asset so it keeps its own config DB
export SPRING_DATASOURCE_URL="$DB_URL"
nohup "$API_JAVA_HOME/bin/java" $API_OPTS \
    -jar "$DATA_API_JAR" --spring.profiles.active=h2 --server.port="$API_PORT" \
    > "$RUN/data-api.out.log" 2>&1 &
echo $! > "$RUN/data-api.pid"
unset SPRING_DATASOURCE_URL

wait_port "$API_PORT" 180 || { echo "data-api failed to start, see $RUN/data-api.out.log" >&2; exit 1; }

[ -f "$ASSET_JAR" ] || { echo "missing $ASSET_JAR - build data-asset first (data-asset/build.cmd or mvn -f data-asset/pom.xml package)" >&2; exit 1; }
echo "starting data-asset on $ASSET_PORT ..."
export OM_BASE_URL="${OM_BASE_URL:-http://127.0.0.1:8585}"
export DS_BASE_URL="${DS_BASE_URL:-http://127.0.0.1:$DS_PORT/dolphinscheduler}"
# never trust the debug header on a shared host
export ASSET_AUTH_MODE="${ASSET_AUTH_MODE:-ds}"
cd "$(dirname "$ASSET_JAR")/.."
ASSET_DB="$(cd "$(dirname "$ASSET_JAR")/.." && pwd)/data/data-asset"
nohup "$ASSET_JAVA_HOME/bin/java" $ASSET_OPTS \
    -jar "$ASSET_JAR" --server.port="$ASSET_PORT" \
    --spring.datasource.url="jdbc:h2:file:$ASSET_DB;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH" \
    > "$RUN/data-asset.out.log" 2>&1 &
echo $! > "$RUN/data-asset.pid"

wait_port "$ASSET_PORT" 120 || { echo "data-asset failed to start, see $RUN/data-asset.out.log" >&2; exit 1; }

echo "up: DS UI http://127.0.0.1:$DS_PORT/dolphinscheduler/ui/  data-api http://127.0.0.1:$API_PORT/data-api/health"
echo "    data-asset http://127.0.0.1:$ASSET_PORT/data-asset/admin/health (needs OpenMetadata on $OM_BASE_URL)"
echo "stop: $ROOT/deploy/local/stop-local.sh"
