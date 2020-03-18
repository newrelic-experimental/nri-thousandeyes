#!/bin/bash

#update the APP_HOME 
export APP_HOME=.

#Use one of the following args to start the monitor as a service that can be invoked by the infra plugin OR as a service reporting directly to Insights
ARGS="-Dnewrelic.platform.config.dir=$APP_HOME/config -Dnewrelic.platform.service.mode=INSIGHTS"

# ***********************************************
# DO NOT EDIT BELOW THIS LINE
# ***********************************************

export CLASSPATH=$APP_HOME/config:$APP_HOME/plugin.jar

MAIN_CLASS=com.newrelic.infra.te.TeMonitor

exec java $ARGS -cp $CLASSPATH $MAIN_CLASS
