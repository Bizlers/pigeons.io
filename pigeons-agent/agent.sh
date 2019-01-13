#!/bin/sh

# Setup variables
EXEC=/usr/bin/jsvc
JAVA_HOME=/usr/lib/jvm/java-8-oracle
CLASS_PATH="/usr/share/java/commons-daemon.jar":"/home/pawan/git/labs.bizlers/pigeons-agent/target/pigeons-agent-1.6.1-SNAPSHOT.jar"
CLASS=com.bizlers.pigeons.agent.core.AgentDaemon
USER=root
PID=/tmp/agent.pid
LOG_OUT=/tmp/agent.out
LOG_ERR=/tmp/agent.err

do_exec()
{
    $EXEC -home "$JAVA_HOME" -cp $CLASS_PATH -user $USER -outfile $LOG_OUT -errfile $LOG_ERR -pidfile $PID $1 $CLASS
}

case "$1" in
    start)
        do_exec
            ;;
    stop)
        do_exec "-stop"
            ;;
    restart)
        if [ -f "$PID" ]; then
            do_exec "-stop"
            do_exec
        else
            echo "service not running, will do nothing"
            exit 1
        fi
            ;;
    *)
            echo "usage: daemon {start|stop|restart}" >&2
            exit 3
            ;;
esac
