package com.madrobot.net;

import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import com.madrobot.net.HttpConstants.HttpMethod;
import com.madrobot.net.util.cache.CachingHttpClient;

/**
 * 
 * @author elton.stephen.kent
 * 
 */
public class HttpHelperSettings {

	/**
	 * Default HTTP payload buffer size 4Kb
	 */
	private int defaultBufferSize = 1042 * 4;

	private boolean expectContinue = true;

	private HttpMethod httpMethod = HttpMethod.GET;

	private boolean isSingleCookieHeader = false;

	private HttpClient httpClient = new DefaultHttpClient();
	{
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_SOCKET_TIME_OUT_PARAM, getSocketTimeout());
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_SINGLE_COOKIE_PARAM, isSingleCookieHeader());
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_EXPECT_CONTINUE_PARAM, isExpectContinue());
	}

	/**
	 * HTTP session timeout. Default is 30 seconds
	 */
	private int socketTimeout = 30000;

	public int getDefaultBufferSize() {
		return defaultBufferSize;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	};

	public HttpMethod getHttpMethod() {
		return httpMethod;
	}

	public int getSocketTimeout() {
		return socketTimeout;
	}

	public boolean isExpectContinue() {
		return expectContinue;
	}

	public boolean isSingleCookieHeader() {
		return isSingleCookieHeader;
	}

	/**
	 * default: 4kb
	 */
	public void setDefaultBufferSize(int defaultBufferSize) {
		this.defaultBufferSize = defaultBufferSize;
	}

	/**
	 * default :true
	 * 
	 * @param expectContinue
	 */
	public void setExpectContinue(boolean expectContinue) {
		this.expectContinue = expectContinue;
	}

	/**
	 * Set the HttpClient implementation to use.
	 * 
	 * @see CachingHttpClient
	 * @see DefaultHttpClient
	 * @param httpClient
	 */
	public void setHttpClient(HttpClient httpClient) {
		this.httpClient = httpClient;
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_SOCKET_TIME_OUT_PARAM, getSocketTimeout());
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_SINGLE_COOKIE_PARAM, isSingleCookieHeader());
		httpClient.getParams().setParameter(
				HttpConstants.HTTP_EXPECT_CONTINUE_PARAM, isExpectContinue());
	}

	public void setHttpMethod(HttpMethod httpMethod) {
		this.httpMethod = httpMethod;
	}

	public void setSingleCookieHeader(boolean isSingleCookieHeader) {
		this.isSingleCookieHeader = isSingleCookieHeader;
	}

	/**
	 * default: 30 secs
	 * 
	 * @param timeout
	 */
	public void setSocketTimeout(int timeout) {
		this.socketTimeout = timeout;
	}

}
