#!/bin/sh
# data-api waits for DS (schema owner) before booting, then shares the same H2 file.
set -eu

mkdir -p /opt/data-api/logs

until [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:12345/dolphinscheduler/ 2>/dev/null)" != "000" ]; do
    sleep 3
done

export SPRING_DATASOURCE_URL='jdbc:h2:file:/opt/ds/data/dsdb;MODE=MySQL;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=true'

exec java -Xms256m -Xmx512m \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone="${TZ:-Asia/Shanghai}" \
    -jar /opt/data-api/dolphinscheduler-data-api.jar \
    --spring.profiles.active=h2 \
    --server.port=12346
