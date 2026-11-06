#!/usr/bin/env bash
set -uo pipefail

RUN="$(cd "$(dirname "$0")" && pwd)/run"

for name in data-asset data-api ds; do
    pid_file="$RUN/$name.pid"
    if [ -f "$pid_file" ]; then
        pid="$(cat "$pid_file")"
        kill "$pid" 2>/dev/null || true
        rm -f "$pid_file"
        echo "stopped $name (pid $pid)"
    fi
done

sleep 2
for port in 12347 12346 12345; do
    pid="$(ss -ltnp 2>/dev/null | awk -v p=":$port" '$4 ~ p {print $0}' | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1)"
    if [ -n "${pid:-}" ]; then
        kill "$pid" 2>/dev/null || true
        echo "released port $port (pid $pid)"
    fi
done
