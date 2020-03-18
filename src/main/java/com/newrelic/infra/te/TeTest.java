/*
 * All components of this product are Copyright (c) 2018 New Relic, Inc.  All rights reserved.
 * Certain inventions disclosed in this file may be claimed within patents owned or patent applications filed by New Relic, Inc. or third parties.
 * Subject to the terms of this notice, New Relic grants you a nonexclusive, nontransferable license, without the right to sublicense, to (a) install and execute one copy of these files on any number of workstations owned or controlled by you and (b) distribute verbatim copies of these files to third parties.  You may install, execute, and distribute these files and their contents only in conjunction with your direct use of New Relic’s services.  These files and their contents shall not be used in conjunction with any other product or software that may compete with any New Relic product, feature, or software. As a condition to the foregoing grant, you must provide this notice along with each copy you distribute and you must not remove, alter, or obscure this notice.  In the event you submit or provide any feedback, code, pull requests, or suggestions to New Relic you hereby grant New Relic a worldwide, non-exclusive, irrevocable, transferable, fully paid-up license to use the code, algorithms, patents, and ideas therein in our products.  
 * All other use, reproduction, modification, distribution, or other exploitation of these files is strictly prohibited, except as may be set forth in a separate written license agreement between you and New Relic.  The terms of any such license agreement will control over this notice.  The license stated above will be automatically terminated and revoked if you exceed its scope or violate any of the terms of this notice.
 * This License does not grant permission to use the trade names, trademarks, service marks, or product names of New Relic, except as required for reasonable and customary use in describing the origin of this file and reproducing the content of this notice.  You may not mark or brand this file with any trade name, trademarks, service marks, or product names other than the original brand (if any) provided by New Relic.
 * Unless otherwise expressly agreed by New Relic in a separate written license agreement, these files are provided AS IS, WITHOUT WARRANTY OF ANY KIND, including without any implied warranties of MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, or NON-INFRINGEMENT.  As a condition to your use of these files, you are solely responsible for such use. New Relic will have no liability to you for direct, indirect, consequential, incidental, special, or punitive damages or for lost profits or data.
 */
package com.newrelic.infra.te;

import java.util.Arrays;

public class TeTest {

	private int testId;
	private String testName = null;
	private String type = null;
	private String server = null;
	private String protocol = null;
	private int networkMeasurements = 0;
	private int mtuMeasurements = 0;
	private int interval = 60;
	
	private TeApiLink[] apiLinks = new TeApiLink[0];
	public int getTestId() {
		return testId;
	}
	public void setTestId(int testId) {
		this.testId = testId;
	}
	public String getTestName() {
		return testName;
	}
	public void setTestName(String testName) {
		this.testName = testName;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getServer() {
		return server;
	}
	public void setServer(String server) {
		this.server = server;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public int getNetworkMeasurements() {
		return networkMeasurements;
	}
	public void setNetworkMeasurements(int networkMeasurements) {
		this.networkMeasurements = networkMeasurements;
	}
	public int getMtuMeasurements() {
		return mtuMeasurements;
	}
	public void setMtuMeasurements(int mtuMeasurements) {
		this.mtuMeasurements = mtuMeasurements;
	}
	public TeApiLink[] getApiLinks() {
		return apiLinks;
	}
	public void setApiLinks(TeApiLink[] apiLinks) {
		this.apiLinks = apiLinks;
	}
	public int getInterval() {
		return interval;
	}
	public void setInterval(int interval) {
		this.interval = interval;
	}
	
	@Override
	public String toString() {
		return "TETest [testId=" + testId + ", testName=" + testName + ", type=" + type + ", server=" + server
				+ ", protocol=" + protocol + ", networkMeasurements=" + networkMeasurements + ", mtuMeasurements="
				+ mtuMeasurements + ", apiLinks=" + Arrays.toString(apiLinks) + "]";
	}	
	
}
