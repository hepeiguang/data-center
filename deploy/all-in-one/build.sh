#!/usr/bin/env bash
# Stage the DS/ST distributions and data-api jar, then build the all-in-one image.
set -euo pipefail

TAG="${TAG:-data-center-all-in-one:3.4.3}"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
DS_HOME="${DS_HOME:-/opt/apache-dolphinscheduler-3.4.3-bin}"
ST_HOME="${ST_HOME:-/opt/apache-seatunnel-3.0.0}"
DATA_API_JAR="${DATA_API_JAR:-$ROOT/dolphinscheduler/dolphinscheduler-data-api/target/dolphinscheduler-data-api-3.4.3.jar}"
MAGIC_DIR="${MAGIC_DIR:-$ROOT/dolphinscheduler/dolphinscheduler-data-api/data/magic-api}"

for p in "$DS_HOME" "$ST_HOME" "$DATA_API_JAR" "$MAGIC_DIR"; do
    [ -e "$p" ] || { echo "missing artifact: $p" >&2; exit 1; }
done

BUILD="$ROOT/deploy/all-in-one/build"
rm -rf "$BUILD"
mkdir -p "$BUILD/data-api/data"

echo "staging DolphinScheduler..."
cp -a "$DS_HOME" "$BUILD/ds"
echo "staging SeaTunnel..."
cp -a "$ST_HOME" "$BUILD/seatunnel"
echo "staging data-api..."
cp "$DATA_API_JAR" "$BUILD/data-api/dolphinscheduler-data-api.jar"
cp -a "$MAGIC_DIR" "$BUILD/data-api/data/magic-api"

PLUGIN_JAR="$ROOT/dolphinscheduler/dolphinscheduler-task-plugin/dolphinscheduler-task-openmetadata/target/dolphinscheduler-task-openmetadata-3.4.3.jar"
if [ -f "$PLUGIN_JAR" ]; then
    mkdir -p "$BUILD/ds/plugins/task-plugins"
    cp "$PLUGIN_JAR" "$BUILD/ds/plugins/task-plugins/"
    echo "staged OpenMetadata task plugin"
fi
UI_DIST="$ROOT/dolphinscheduler/dolphinscheduler-ui/dist"
if [ -d "$UI_DIST" ]; then
    rm -rf "$BUILD/ds/ui"
    cp -a "$UI_DIST" "$BUILD/ds/ui"
    echo "staged rebuilt DolphinScheduler UI"
fi

find "$BUILD" -type d -name logs -prune -exec sh -c 'rm -rf "$1"/*' _ {} \;
find "$BUILD" -type f \( -name '*.log' -o -name 'gc-*.log' \) -delete

cd "$ROOT/deploy/all-in-one"
docker build -t "$TAG" .

echo "built $TAG"
echo "run: docker compose -f deploy/all-in-one/docker-compose.yml up -d"
