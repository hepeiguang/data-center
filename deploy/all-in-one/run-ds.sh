#!/bin/sh
# DolphinScheduler standalone in the foreground so supervisor can track the JVM.
set -eu

mkdir -p /opt/ds/data /opt/ds/logs

# The bundled H2 schema script drops/recreates tables, so only run it on a fresh database.
if [ ! -f /opt/ds/data/dsdb.mv.db ]; then
    export SPRING_SQL_INIT_MODE=always
else
    export SPRING_SQL_INIT_MODE=never
fi

CP="/opt/ds/standalone-server/conf"
CP="$CP:/opt/ds/api-server/libs/*"
CP="$CP:/opt/ds/master-server/libs/*"
CP="$CP:/opt/ds/worker-server/libs/*"
CP="$CP:/opt/ds/alert-server/libs/*"
CP="$CP:/opt/ds/plugins/alert-plugins/*"
CP="$CP:/opt/ds/plugins/datasource-plugins/*"
CP="$CP:/opt/ds/plugins/storage-plugins/*"
CP="$CP:/opt/ds/plugins/task-plugins/*"
CP="$CP:/opt/ds/standalone-server/libs/*"

exec java -server -Xms512m -Xmx1024m -Xmn256m \
    -XX:+IgnoreUnrecognizedVMOptions \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone="${TZ:-Asia/Shanghai}" \
    -cp "$CP" org.apache.dolphinscheduler.StandaloneServer
