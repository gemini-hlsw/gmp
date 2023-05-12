#!/bin/bash
#
# gmp-server control script
#
# usage gmp-server-ctl.sh {start|stop|status|kill}
#
set -e # stop on errors
set -u # Don't allow using non defined variables

# Vars that are set at build time

PAX_RUNNER_VERSION=15.0.8
GMP_VERSION=15.0.8-SNAPSHOT

# Overrides locally JAVA_HOME to use java 8
export JAVA_HOME=/usr/lib/jvm/java-1.8.0
JAVA8=$JAVA_HOME/bin/java
#
# Confirm that java is available
which $JAVA8 > /dev/null || { echo "Need java in PATH to run"; exit 1; }

# Verify existing variables
if [ -z ${GMP_ROOT:-} ]; then
    tmp=`pwd`
    GMP_ROOT="${tmp%/*}"
    echo "GMP_ROOT not set. Using $GMP_ROOT"
fi
if ! [ -d $GMP_ROOT ]; then
    echo "$GMP_ROOT directory not found"
    exit -1
fi
if [ -z "${HOME:-}" ]; then
    HOME=$GMP_ROOT
fi

# App variables
app_name=gmp-server
app_root=${GMP_ROOT}
pid_file=${app_root}/bin/${app_name}.pid
log_dir=${app_root}/logs/
log_file=${log_dir}/${app_name}.out

# Check log dirs
if ! [ -d ${log_dir} ]; then
    echo "Log dir is missing, creating it at ${log_dir}"
    mkdir -p ${log_dir}
fi

# check rpm root is available
if ! [ -e ${app_root} ]; then
    echo "gmp-server directory ${app_root} not found. Check your GPI_ROOT env variable"
    exit -1
fi

# pax-runner needs this dir to run
if ! [ -d $HOME/.pax/runner ]; then
    mkdir -p $HOME/.pax/runner
    touch $HOME/.pax/runner/org.ops4j.pax.runner.daemon.password.file
fi

#
# - get PID from .pid file if it exists and check if it is running
#
pid=`[[ -e ${pid_file} ]] && cat ${pid_file} || echo "NO_PID_FILE"`
set +e # disable because egrep will return 1 on mismatch
pid_isrunning=`ps -eo pid | egrep ^[[:space:]]*${pid}$`
# Check pax-runner lock is not there
if [ -z ${pid_isrunning} ] && [ -e $HOME/.pax/runner/org.ops4j.pax.runner.daemon.lock ]; then
    echo "Seems GMP is not running but the lock file is still in place"
    echo "If you are sure GMP is not running delete $HOME/.pax/runner/org.ops4j.pax.runner.daemon.lock"
fi

set -e # reenable

#
# Start the GMP server if not already running
#
function startContainer() {
    set +e # disable because egrep will return 1 on mismatch
    pid_isrunning=`ps -eo pid | egrep ^[[:space:]]*${pid}$`
    if [ -z ${pid_isrunning} ]; then
        echo "Starting ${app_name} version $GMP_VERSION"
        # Will start pax-runner as a daemon reading the configuration from the file bin/runner.args
        pushd ${app_root}/bin > /dev/null;
        $JAVA8 -cp ${app_root}/bin/pax-runner-${PAX_RUNNER_VERSION}.jar org.ops4j.pax.runner.daemon.DaemonLauncher --startd &> ${log_file}
        wait $!
        sleep 4
        popd > /dev/null
        # Get the pid with ps
        ps -ef | awk '/java.*org.apache.felix.main.Main$/ {print $2}' > ${pid_file}
        retval=$?
        sleep 10
        echo "Started ${app_name}"
        return $retval
    else
        echo "${app_name} already running with pid ${pid}"
    fi
}

#
# Gracefully stop the GMP
#
function stopContainer() {
  if [ ! -z ${pid_isrunning} ]; then
    echo "Stopping ${app_name} with pid ${pid}"
    $JAVA8 -cp ${app_root}/bin/pax-runner-${PAX_RUNNER_VERSION}.jar org.ops4j.pax.runner.daemon.DaemonLauncher --stop
    counter=0
    while kill -0 "$pid" 2> /dev/null; do
      counter=$((counter+1))
      sleep 1
      if [[ "$counter" -gt 25 ]]; then
        echo "Taking too long to die, forced to kill"
        jstack -l "$pid" > $HOME/.pax/dump_${pid}
        kill -9 "$pid"
        sleep 5
      fi
    done
    #wait $!
    retval=$?
    #sleep 10
    return $retval
  else
    echo "${app_name} not running"
  fi
}

#
# Kill the GMP process
#
function killContainer() {
  if [ ! -z ${pid_isrunning} ]; then
    echo "Killing ${app_name} with pid ${pid}"
    kill `cat ${pid_file}`
  else
    echo "${app_name} not running"
  fi
}

#
# Check if GMP is still running
#
function containerStatus() {
  if [ ! -z ${pid_isrunning} ]; then
    echo "${app_name} is running with pid ${pid}"
  else
    echo "${app_name} not running"
  fi
}

# Reenable as the script could be called without parameters
set +u

#
# - main command dispatch ...
#
case "$1" in
    start)
      startContainer
      ;;
    stop)
      stopContainer
      ;;
    kill)
      killContainer
      ;;
    restart)
      stopContainer
      startContainer
      ;;
    status)
      containerStatus
      ;;
    *)
      echo "Usage: ${app_name} {start|stop|status|kill}"
      exit 1
      ;;
esac
# End of main
exit 0
