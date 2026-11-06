#!/bin/sh
# data-api waits for DS (schema owner) before booting, then shares the same H2 file.
set -eu

mkdir -p /opt/data-api/logs

until [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:12345/dolphinscheduler/ 2>/dev/null)" != "000" ]; do
    sleep 3
done

export SPRING_DATASOURCE_URL='jdbc:h2:file:/opt/ds/data/dsdb;MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true'

# Boot 3 needs 17+; the image default JDK stays 11 for DS/SeaTunnel (see Dockerfile)
API_JAVA="${API_JAVA:-/usr/lib/jvm/java-17-openjdk-amd64/bin/java}"
if [ ! -x "$API_JAVA" ]; then
    echo "data-api needs a JDK 17+ runtime, not found: $API_JAVA (override with API_JAVA)" >&2
    exit 1
fi

exec "$API_JAVA" -Xms256m -Xmx512m \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone="${TZ:-Asia/Shanghai}" \
    -jar /opt/data-api/dolphinscheduler-data-api.jar \
    --spring.profiles.active=h2 \
    --server.port=12346
