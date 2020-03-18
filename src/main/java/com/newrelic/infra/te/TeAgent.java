/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.te;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.DatatypeConverter;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.infra.publish.api.Agent;
import com.newrelic.infra.publish.api.InventoryReporter;
import com.newrelic.infra.publish.api.MetricReporter;
import com.newrelic.infra.publish.api.metrics.AttributeMetric;
import com.newrelic.infra.publish.api.metrics.GaugeMetric;
import com.newrelic.infra.publish.api.metrics.Metric;
import com.newrelic.insights.publish.ClientConnectionConfiguration;
import com.newrelic.insights.publish.Event;

public class TeAgent extends Agent {
	private static final Logger logger = LoggerFactory.getLogger(TeAgent.class);

	ObjectMapper mapper = new ObjectMapper();
	private static final String PROPERTY_VALUE_HEADER_CONTENT_TYPE = "application/json";
	private CloseableHttpClient httpclient = null;
	private String serverAuthUser = "";
	private String serverAuthPassword = "";
	private int pauseInterval = 60;
	
	private Map<Integer, Integer> lastRoundMap_metrics = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> lastRoundMap_pathVis = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> lastRoundMap_dns = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> lastRoundMap_bgpMetrics = new HashMap<Integer, Integer>();
	
	private Map<Integer, Long> lastMonitorRunMap = new HashMap<Integer, Long>();
	
	
	public TeAgent(String serverAuthUser, String serverAuthPassword, ClientConnectionConfiguration httpConfig, int pauseInterval) {
		super();
		this.serverAuthUser = serverAuthUser;
		this.serverAuthPassword = serverAuthPassword;
		this.pauseInterval = pauseInterval;
		init(httpConfig);
	}
	
	public void init(ClientConnectionConfiguration connConfig) {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		RequestConfig requestConfig;
		if (connConfig.isUseProxy()) {
			HttpHost proxy = new HttpHost(connConfig.getProxyHost(), connConfig.getProxyPort(),
					connConfig.getProxyScheme());
			requestConfig = RequestConfig.custom().setConnectionRequestTimeout(connConfig.getConnectionRequestTimeout())
					.setConnectTimeout(connConfig.getConnectTimeout()).setSocketTimeout(connConfig.getSocketTimeout())
					.setProxy(proxy).build();
		} else {
			requestConfig = RequestConfig.custom().setConnectionRequestTimeout(connConfig.getConnectionRequestTimeout())
					.setConnectTimeout(connConfig.getConnectTimeout()).setSocketTimeout(connConfig.getSocketTimeout())
					.build();
		}

		List<Header> headers = new ArrayList<Header>();

		Header contentTypeHeader = new BasicHeader(HttpHeaders.CONTENT_TYPE, PROPERTY_VALUE_HEADER_CONTENT_TYPE);

		if ((connConfig.getProxyUsername() != null) && (connConfig.getProxyPassword() != null)) {
			String proxyUserAndPass = ((String) connConfig.getProxyUsername()).trim() + ":"
					+ ((String) connConfig.getProxyPassword()).trim();
			headers.add(
					new BasicHeader("Proxy-Authorization", "Basic " + DatatypeConverter.parseString(proxyUserAndPass)));
		}

		headers.add(contentTypeHeader);

		CredentialsProvider provider = new BasicCredentialsProvider();
		logger.info("Setting thousand eyes username " + serverAuthUser);
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(serverAuthUser,
				serverAuthPassword);
		provider.setCredentials(AuthScope.ANY, credentials);

		httpclient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider)
				.setDefaultRequestConfig(requestConfig).setDefaultHeaders(headers).build();
	}

	@Override
	public void populateMetrics(MetricReporter metricReporter) throws Exception {
		TeTest[] tests = getAllTeTests();
		
		if (tests != null) {
			for (TeTest tetest : tests) {
				long lastRunTime = lastMonitorRunMap.getOrDefault(tetest.getTestId(), 0L);
				long timeNow = System.currentTimeMillis();
				
				if (lastRunTime == 0) {
					processTest(tetest, metricReporter);
				} else {
					if (tetest.getInterval() > 240) {
						if ((timeNow - lastRunTime) > tetest.getInterval() * 400)  {
							processTest(tetest, metricReporter);
						} else {
							//System.out.println("skipping run " + (timeNow - lastRunTime)/1000);
						}
					} else {
						processTest(tetest, metricReporter);
					}
				}
			}
		} else {
			logger.error("No tests found or error occurred");
		}
	}

	private void processTest(TeTest tetest, MetricReporter metricReporter) {
		lastMonitorRunMap.put(tetest.getTestId(), System.currentTimeMillis());
		int window = tetest.getInterval();
		for (TeApiLink link : tetest.getApiLinks()) {
			String linkRef = link.getHref();
			if (!link.getRel().equalsIgnoreCase("self")) {
				boolean no_pause = processTeApiLink(tetest, linkRef, metricReporter, window);
				if (!no_pause) {
					try {
						logger.error("HTTP 429 too many requests, pausing for " + pauseInterval  + " seconds");
						Thread.sleep(pauseInterval * 1000);
					} catch (Exception e) {}
				}
			}
		}
	}

	@Override
	public void populateInventory(InventoryReporter inventoryReporter) throws Exception {
	}

	@Override
	public void dispose() throws Exception {
		if (httpclient != null) {
			httpclient.close();
		}
	}
	
	public TeTest[] getAllTeTests() {
		TeTestResponse teTestResponse = new TeTestResponse();
		HttpGet getRequest = new HttpGet("https://api.thousandeyes.com/v6/tests.json");
		CloseableHttpResponse response = null;
		try {
			response = httpclient.execute(getRequest);
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			logger.debug("Response is " + response.getStatusLine());
			
			if (status >= 200 && status < 300) {
				try {
					if (entity != null) {
						InputStream input = entity.getContent();
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
						TeTestResponse tr = mapper.readValue(bufferedReader, TeTestResponse.class);
						teTestResponse = tr;
						logger.trace(teTestResponse.toString());	
					} else {
						logger.error("Response error, no content in reponse body");
					}
				} finally {
					EntityUtils.consumeQuietly(entity);
					response.close();
				}
			} else if (status == 401) {
				//logger.error("401 Unauthorized");
				//be careful with 401 unauthorized as it may lock up the account if these requests are repeated made 
				return null;
			} else if (status == 429) {
				return null;
			} else {
				String responseBody = EntityUtils.toString(response.getEntity());
				logger.error("Response error is " + responseBody);
				EntityUtils.consumeQuietly(entity);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
				}
			}
		}
		return teTestResponse.getTeTests();
	}

	public boolean processTeApiLink(TeTest teTest, String teTestApiLink, MetricReporter metricReporter, int window) {
		//logger.debug("\nProcessing endpoint: " + teTestApiLink);
		List<Event> events = new ArrayList<Event>();
		
		HttpGet getRequest = new HttpGet(teTestApiLink + ".json?window=" + window + "s");
		CloseableHttpResponse response = null;
		try {
			response = httpclient.execute(getRequest);
			int status = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			// logger.debug("Response is " + response.getStatusLine());
			if (status >= 200 && status < 300) {
				try {
					if (entity != null) {
						InputStream input = entity.getContent();
						BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
						Map<String, Object> testDataRoot = mapper.readValue(bufferedReader,
								new TypeReference<Map<String, Object>>() {
								});
						for (Entry<String, Object> testDataSection : testDataRoot.entrySet()) {
							String testDataSectionName = testDataSection.getKey();
							
							if (testDataSection.getKey().equalsIgnoreCase("net")) {
								Object netObj = testDataSection.getValue();
								if (netObj instanceof Map) {
									Map<String, Object> net = (Map<String, Object>) netObj;
									for (Iterator it = net.entrySet().iterator(); it.hasNext();) {
										Entry<String, Object> metricElement = (Entry<String, Object>) it.next();
										String metricElementName = metricElement.getKey();
										Object metricElementValueObj = metricElement.getValue();
										if (metricElementName.equalsIgnoreCase("test")) {
										} else if (metricElementName.equalsIgnoreCase("metrics")) {
											logger.trace("Processing element: " + testDataSectionName + "/"
													+ metricElementName);
											if (metricElementValueObj instanceof ArrayList) {
												ArrayList metricElementValue = (ArrayList) metricElementValueObj;
												int lastRoundId = lastRoundMap_metrics.getOrDefault(teTest.getTestId(), 0);
												processMetricElement("TENetworkMetricSample", teTest, metricElementValue, metricReporter, lastRoundId, lastRoundMap_metrics);
											}
										} else if (metricElementName.equalsIgnoreCase("pathVis")) {
											logger.trace("Processing element: " + testDataSectionName + "/"
													+ metricElementName);
											if (metricElementValueObj instanceof ArrayList) {
												ArrayList metricElementValue = (ArrayList) metricElementValueObj;
												int lastRoundId = lastRoundMap_pathVis.getOrDefault(teTest.getTestId(), 0);
												processMetricElement("TENetworkPathVizSample", teTest, metricElementValue, metricReporter, lastRoundId, lastRoundMap_pathVis);
											}
										} else if (metricElementName.equalsIgnoreCase("bgpMetrics")) {
											logger.trace("Processing element: " + testDataSectionName + "/"
													+ metricElementName);
											if (metricElementValueObj instanceof ArrayList) {
												ArrayList metricElementValue = (ArrayList) metricElementValueObj;
												int lastRoundId = lastRoundMap_bgpMetrics.getOrDefault(teTest.getTestId(), 0);
												processMetricElement("TENetworkBGPSample", teTest, metricElementValue, metricReporter, lastRoundId, lastRoundMap_bgpMetrics);
											}
										} else {
											logger.trace("Not processing element: " + testDataSectionName + "/"
													+ metricElementName);
										}
									}
								}
							} else if (testDataSection.getKey().equalsIgnoreCase("dns")) {
								Object netObj = testDataSection.getValue();
								if (netObj instanceof Map) {
									Map<String, Object> net = (Map<String, Object>) netObj;
									for (Iterator it = net.entrySet().iterator(); it.hasNext();) {
										Entry<String, Object> metricElement = (Entry<String, Object>) it.next();
										String metricElementName = metricElement.getKey();
										Object metricElementValueObj = metricElement.getValue();
										if (metricElementName.equalsIgnoreCase("test")) {
										} else if (metricElementName.equalsIgnoreCase("server")) {
											logger.trace("Processing element: " + testDataSectionName + "/"
													+ metricElementName);
											if (metricElementValueObj instanceof ArrayList) {
												ArrayList metricElementValue = (ArrayList) metricElementValueObj;
												int lastRoundId = lastRoundMap_dns.getOrDefault(teTest.getTestId(), 0);
												processMetricElement("TEDNSSample", teTest, metricElementValue, metricReporter, lastRoundId, lastRoundMap_dns);
											}
										} else {
											logger.trace("Not processing element: " + testDataSectionName + "/"
													+ metricElementName);
										}
									}
								}
							} else {
								//logger.debug("Skipping root element: " + testDataSection.getKey());
							}
						}
					}
				} finally {
					EntityUtils.consumeQuietly(entity);
				}
			} else if (status == 401) {
				//logger.error("401 Unauthorized");
				//be careful with 401 unauthorized as it may lock up the account if these requests are repeated made 
				logger.error("401 unauthorized, skipping the rest of metrics for this test" );
				return true;
			} else if (status == 429) {
				//pause if status is '429 Too Many Requests'
				return false;
			} else {
				String responseBody = EntityUtils.toString(response.getEntity());
				logger.error("Response error is " + responseBody);
				EntityUtils.consumeQuietly(entity);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
				}
			}
		}
		return true;
	}

	private void processMetricElement(String metricSetName, TeTest teTest, ArrayList metricElementValue, MetricReporter metricReporter, int previousRoundId, Map<Integer, Integer> lastRoundMap) {
		for (Object metricPropertiesObj : metricElementValue) {
			boolean isStale = false;
			List<Metric> metricSet = new ArrayList<Metric>();
			Map<String, Object> metricProperties = (Map<String, Object>) metricPropertiesObj;
			for (String propKey : metricProperties.keySet()) {
				Object propValue = metricProperties.get(propKey);
				if (propKey.equalsIgnoreCase("agentId")) {
					metricSet.add(new AttributeMetric(propKey, propValue.toString()));	
				} else if (propKey.equalsIgnoreCase("roundId")) {
					Integer currentRoundId = (Integer) propValue;
					
					Integer _lastestRoundId = lastRoundMap.getOrDefault(teTest.getTestId(), 0);
					if (currentRoundId > _lastestRoundId) {
						lastRoundMap.put(teTest.getTestId(), currentRoundId);
					}
					
					if ( (previousRoundId == 0) ||(currentRoundId <= previousRoundId)) {
						logger.trace(currentRoundId + " is a zero or a stale roundid for test " + teTest.getTestId() + " #" + metricSetName +  ". The lastRoundId reported already is " + previousRoundId);
						isStale = true;
						break;
					} else {
						logger.trace(currentRoundId + " being reported for test " + teTest.getTestId() + " #" + metricSetName +  ". The lastRoundId reported was " + previousRoundId);
					}
					metricSet.add(new AttributeMetric("roundId", Integer.toString(currentRoundId)));
				} else if ((propValue.getClass() == Double.class) 
						|| (propValue.getClass() == Integer.class)
						|| (propValue.getClass() == Long.class)
						|| (propValue.getClass() == Float.class)) {
					metricSet.add(new GaugeMetric(propKey, (Number) propValue));
				} else {
					metricSet.add(new AttributeMetric(propKey, propValue.toString()));		
				}
			}
			metricSet.add(new AttributeMetric("testId", Integer.toString(teTest.getTestId())));
			metricSet.add(new AttributeMetric("testName", teTest.getTestName()));
			metricSet.add(new AttributeMetric("testType", teTest.getType()));
			metricSet.add(new AttributeMetric("server", teTest.getServer()));
			metricSet.add(new AttributeMetric("protocol", teTest.getProtocol()));
			
			if (!isStale) {
				metricReporter.report(metricSetName, metricSet);
			}
		}
	}

}
