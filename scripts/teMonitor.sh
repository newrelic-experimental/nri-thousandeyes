#!/bin/bash

START_SCRIPT={{ path to start_script }}
PID_FILE={{ path to pid_file }}

# ***********************************************
# ***********************************************

ARGS="" # optional start script arguments
DAEMON=$START_SCRIPT

# colors
red='\e[0;31m'
green='\e[0;32m'
yellow='\e[0;33m'
reset='\e[0m'

echoRed() { echo -e "${red}$1${reset}"; }
echoGreen() { echo -e "${green}$1${reset}"; }
echoYellow() { echo -e "${yellow}$1${reset}"; }

start() {
  PID=`$DAEMON $ARGS > /dev/null 2>&1 & echo $!`
}

case "$1" in
start)
    if [ -f $PID_FILE ]; then
        PID=`cat $PID_FILE`
        if [ -z "`ps axf | grep -w ${PID} | grep -v grep`" ]; then
            start
        else
            echoYellow "Already running [$PID]"
            exit 0
        fi
    else
        start
    fi

    if [ -z $PID ]; then
        echoRed "Failed starting"
        exit 3
    else
        echo $PID > $PID_FILE
        echoGreen "Started [$PID]"
        exit 0
    fi
;;

status)
    if [ -f $PID_FILE ]; then
        PID=`cat $PID_FILE`
        if [ -z "`ps axf | grep -w ${PID} | grep -v grep`" ]; then
            echoRed "Not running (process dead but pidfile exists)"
            exit 1
        else
            echoGreen "Running [$PID]"
            exit 0
        fi
    else
        echoRed "Not running"
        exit 3
    fi
;;

stop)
    if [ -f $PID_FILE ]; then
        PID=`cat $PID_FILE`
        if [ -z "`ps axf | grep -w ${PID} | grep -v grep`" ]; then
            echoRed "Not running (process dead but pidfile exists)"
            exit 1
        else
            PID=`cat $PID_FILE`
            kill -HUP $PID
            echoGreen "Stopped [$PID]"
            rm -f $PID_FILE
            exit 0
        fi
    else
        echoRed "Not running (pid not found)"
        exit 3
    fi
;;

restart)
    $0 stop
    $0 start
;;

*)
    echo "Usage: $0 {status|start|stop|restart}"
    exit 1
esac