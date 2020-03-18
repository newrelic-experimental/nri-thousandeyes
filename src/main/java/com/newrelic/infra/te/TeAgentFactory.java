/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.te;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.AgentFactory;
import com.newrelic.infra.publish.api.Runner;
import com.newrelic.insights.publish.ClientConnectionConfiguration;

public class TeAgentFactory extends AgentFactory {
	private static final Logger logger = LoggerFactory.getLogger(TeAgentFactory.class);
	public static final String PROXY = "proxy";
	public static final String PROXY_HOST = "proxy_host";
	public static final String PROXY_PORT = "proxy_port";
	public static final String PROXY_USERNAME = "proxy_username";
	public static final String PROXY_PASSWORD = "proxy_password";
	private ClientConnectionConfiguration httpConfig = new ClientConnectionConfiguration();
	
	@Override
	public Agent createAgent(Map<String, Object> agentProperties) throws Exception {
		String name = (String) agentProperties.get("name");
		String username = (String) agentProperties.get("username");
		String password = (String) agentProperties.get("password");	
		
		int pauseInterval = 60;
		if (agentProperties.containsKey("pauseInterval")) {
			pauseInterval  = (Integer) agentProperties.get("pauseInterval") ;	
		}
		
		TeAgent agent = new TeAgent(username, password, httpConfig, pauseInterval);
		return agent;
	}

	@Override
	public void init(Map<String, Object> globalProperties) {
		super.init(globalProperties);
		httpConfig.setMaximumConnectionsPerRoute(6);
		httpConfig.setMaximumConnections(12);
		
		Object proxyObj = globalProperties.get(Runner.PROXY);
		if ((proxyObj != null) && (proxyObj instanceof Map)) {
			logger.info("Reading proxy configuration setting");
			Map<String, Object> proxyProperties = (Map<String, Object>) globalProperties.get(Runner.PROXY);
			
			if (proxyProperties.containsKey(PROXY_HOST)) {
				logger.info("Using proxy");
				httpConfig.setUseProxy(true);
				httpConfig.setProxyScheme("http");
				
				String proxyHost = (String) proxyProperties.get(PROXY_HOST);
				logger.info("Using proxy_host " + proxyHost);
				httpConfig.setProxyHost(proxyHost);
				
				if (proxyProperties.containsKey(PROXY_PORT)) {
					Integer proxyPort = ((Integer) proxyProperties.get(PROXY_PORT));
					
					logger.info("Using proxy_port " + proxyPort);
					httpConfig.setProxyPort(proxyPort);
				}
			}
			if (proxyProperties.containsKey(PROXY_USERNAME)) {
				String proxyUser = (String) proxyProperties.get(PROXY_USERNAME);
				logger.info("Using proxy_username " + proxyUser);
				httpConfig.setProxyUsername(proxyUser);
			}	

			logger.info(proxyProperties.toString());
			if (proxyProperties.containsKey(PROXY_PASSWORD)) {
				logger.info(proxyProperties.toString());
				String proxyPassword = (String) proxyProperties.get(PROXY_PASSWORD);
				logger.info("Using proxy_password " + proxyPassword);
				httpConfig.setProxyPassword(proxyPassword);
			}
		}
	}

	
}
