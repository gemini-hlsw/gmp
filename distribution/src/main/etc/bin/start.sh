#!/bin/bash
#
# Starts the GMP server in the foreground.
#
SCRIPT_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
APP_ROOT="${SCRIPT_PATH%/bin}"

cd "$APP_ROOT"

exec java \
    -Dconf.base="$APP_ROOT/conf" \
    -Dlogs.dir="$APP_ROOT/logs" \
    -Djava.util.logging.config.file="$APP_ROOT/conf/logging.properties" \
    -cp "$APP_ROOT/lib/*" \
    edu.gemini.aspen.gmp.main.GmpMain
