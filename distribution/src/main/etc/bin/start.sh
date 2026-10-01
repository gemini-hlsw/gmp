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
    -Dlogback.configurationFile="$APP_ROOT/conf/logback.xml" \
    -cp "$APP_ROOT/lib/*" \
    edu.gemini.aspen.gmp.main.GmpMain
