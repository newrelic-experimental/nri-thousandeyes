[![New Relic Experimental header](https://github.com/newrelic/open-source-office/raw/master/examples/categories/images/Experimental.png)](https://github.com/newrelic/open-source-office/blob/master/examples/categories/index.md#new-relic-experimental)

# New Relic integration for ThousandEyes
Infra Integration to bridge metrics from ThousandEyes (a network monitoring tool) into Insights

## Monitor requirements

1. Java Runtime version 1.6 or later

## Installation

Unzip the thousandeyes-integration.tar.gz file into the monitor home folder.

```
cd {thousandeyes-integration}
tar xvf  thousandeyes-integration.tar.gz
```

The control script - start.sh and teMonitor.sh - is used for starting, stopping and getting status.
Update start.sh as follows

* Edit the APP_HOME variable in the start.sh script.


### Setup the monitor to run as service

Setting up the application to run as a Java Service is dependent on the platform. Please contact your platform administrator for help setting this up.


## Configuration

### plugin.json

Rename the plugin.template.json to plugin.json. Edit all parameters to your environment. 

The "global" object can contain the overall plugin properties:

* "account_id" - your new relic account id. You can find it in the URL that you use to access newrelic. For example: https://rpm.newrelic.com/accounts/{accountID}/applications
* "insights_mode" - If this monitor is started in Insights mode, then the insights_insert_key here will be used to post metric events. The interval is the time in seconds at which this monitor should schedule executions.
* "infra_mode" - If this monitor is started in Infra (or RPC) mode, then this section contains the RPC service listen port. The infra agent plugin can then connect this to port to query metrics.
* "proxy" - Enter the proxy setting in this section if a proxy is required. See more detail later on in this document.


The "agents" object contains an array of “agent” objects. Each “agent” object has the following properties.

* "name" – any descriptive name for this thousand eyes account being monitored
* "username" - username to use to authenticate to thousand eyes
* "password" – password to use to authenticate to thousand eyes
* "pauseInterval"- thousand eyes has a maximum number of API calls it will respond to per minute. After the limit it crossed, a HTTP 429 is returned. So this interval in seconds is the amount of time for which the monitor should pause before querying again.

```
{
	"global": {
		"account_id": "insert_your_RPM_account_ID_here",
		"insights_mode": {
			"insights_insert_key": "insert_your_insights_insert_key_here",
			"interval": 300
		},
		"infra_mode": {
			"rpc_listener_port": 9001
		}
	},
	"agents": [
	           {
	           	   "name": "descriptive t-eyes account name",
	        	   	   "host": "127.0.0.1",
	        	   	   "username": "t-eyes username",
	        	   	   "password": "t-eyes password",
	        	   	   "pauseInterval": 20
	           }
	]
}
```


## Proxy settings

```
	"proxy": {
			"proxy_host": "enter_proxy_host",
			"proxy_port": 443,
			"proxy_username": "enter_proxy_username",
			"proxy_password": "enter_proxy_password"
	}
```


## Logging

The logging configuration can be controlled using the logback configuration file- ./config/logback.xml

Edit the following block of XML at the end of the logback.xml to change the log level (possible values are INFO, DEBUG, ERROR) and the log ouput(possible values are STDOUT, FILE)
```
    <logger name="com.newrelic" level="INFO" additivity="false">
        <appender-ref ref="STDOUT" />
    </logger>
```

## Metrics and Dashboarding
All metrics collected by this plugin are reported as events whose type start with "te" 







