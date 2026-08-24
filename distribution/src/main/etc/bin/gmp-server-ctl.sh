#!/bin/bash
#
# Control script for the GMP server (plain JVM process; pax-runner is gone).
#
# usage: gmp-server-ctl.sh start|stop|restart|status
#
set -u

SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_ROOT="${SCRIPT_PATH%/bin}"
PID_FILE="$APP_ROOT/gmp-server.pid"
LOG_DIR="$APP_ROOT/logs"
OUT_FILE="$LOG_DIR/gmp-server.out"
STOP_TIMEOUT=25

running() {
    [ -f "$PID_FILE" ] && kill -0 "$(cat "$PID_FILE")" 2> /dev/null
}

start() {
    if running; then
        echo "gmp-server already running (pid $(cat "$PID_FILE"))"
        return 0
    fi
    mkdir -p "$LOG_DIR"
    cd "$APP_ROOT"
    nohup java \
        -Dconf.base="$APP_ROOT/conf" \
        -Dlogs.dir="$LOG_DIR" \
        -Djava.util.logging.config.file="$APP_ROOT/conf/logging.properties" \
        -cp "$APP_ROOT/lib/*" \
        edu.gemini.aspen.gmp.main.GmpMain >> "$OUT_FILE" 2>&1 &
    echo $! > "$PID_FILE"
    echo "gmp-server started (pid $(cat "$PID_FILE"))"
}

stop() {
    if ! running; then
        echo "gmp-server is not running"
        rm -f "$PID_FILE"
        return 0
    fi
    local pid
    pid=$(cat "$PID_FILE")
    kill "$pid"
    for _ in $(seq 1 "$STOP_TIMEOUT"); do
        if ! kill -0 "$pid" 2> /dev/null; then
            rm -f "$PID_FILE"
            echo "gmp-server stopped"
            return 0
        fi
        sleep 1
    done
    echo "gmp-server did not stop after ${STOP_TIMEOUT}s; taking thread dump and killing"
    jstack "$pid" >> "$OUT_FILE" 2>&1 || true
    kill -9 "$pid"
    rm -f "$PID_FILE"
}

status() {
    if running; then
        echo "gmp-server is running (pid $(cat "$PID_FILE"))"
    else
        echo "gmp-server is not running"
        return 3
    fi
}

case "${1:-}" in
    start)   start ;;
    stop)    stop ;;
    restart) stop; start ;;
    status)  status ;;
    *) echo "usage: $0 start|stop|restart|status"; exit 2 ;;
esac
