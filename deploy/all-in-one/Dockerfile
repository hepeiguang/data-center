# All-in-one image: DolphinScheduler standalone + SeaTunnel (local mode) + data-api + nginx
# Build context must contain build/ds, build/seatunnel, build/data-api (see build.sh).
FROM eclipse-temurin:11-jre-jammy

ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
 && apt-get install -y --no-install-recommends nginx supervisor curl tzdata procps \
 && rm -rf /var/lib/apt/lists/* \
 && rm -f /etc/nginx/sites-enabled/default

ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    DS_HOME=/opt/ds \
    SEATUNNEL_HOME=/opt/seatunnel

COPY build/ds/ /opt/ds/
COPY build/seatunnel/ /opt/seatunnel/
COPY build/data-api/ /opt/data-api/
COPY nginx.conf /etc/nginx/nginx.conf
COPY supervisor-all-in-one.conf /etc/supervisor/conf.d/all-in-one.conf
COPY run-ds.sh /opt/ds/run-ds.sh
COPY run-data-api.sh /opt/data-api/run-data-api.sh

# The shipped standalone config points at the developer's Windows paths.
RUN sed -i 's#D:/HPG/workspace/LHYF/data/dsdb#/opt/ds/data/dsdb#g; s#D:/ds/data#/opt/ds/data#g' \
        /opt/ds/standalone-server/conf/application.yaml \
        /opt/ds/standalone-server/conf/common.properties \
 && sed -i 's#fs.defaultFS: file:///tmp/#fs.defaultFS: file:///opt/seatunnel/checkpoint/#g' \
        /opt/seatunnel/config/seatunnel.yaml \
 && chmod +x /opt/ds/run-ds.sh /opt/data-api/run-data-api.sh \
 && mkdir -p /opt/ds/data /opt/ds/logs /opt/data-api/logs /opt/seatunnel/logs /opt/seatunnel/checkpoint \
             /tmp/nginx/client_body /tmp/nginx/proxy /tmp/nginx/fastcgi /tmp/nginx/uwsgi /tmp/nginx/scgi

EXPOSE 8088 12345 12346

VOLUME ["/opt/ds/data", "/opt/ds/logs", "/opt/data-api/logs", "/opt/seatunnel/checkpoint"]

HEALTHCHECK --interval=30s --timeout=5s --start-period=120s --retries=5 \
  CMD curl -fsS http://127.0.0.1:8088/data-api/health || exit 1

CMD ["/usr/bin/supervisord", "-n", "-c", "/etc/supervisor/supervisord.conf"]
