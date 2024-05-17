/*
 * Copyright © 2024 XDEV Software (https://xdev.software)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package software.xdev.mockserver.cors;

import static io.netty.handler.codec.http.HttpMethod.OPTIONS;
import static software.xdev.mockserver.configuration.ServerConfiguration.configuration;

import io.netty.handler.codec.http.HttpHeaderNames;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;


public class CORSHeaders
{
	private static final String ANY_ORIGIN = "*";
	private static final String NULL_ORIGIN = "null";
	
	private final String corsAllowOrigin;
	private final String corsAllowHeaders;
	private final String corsAllowMethods;
	private final boolean corsAllowCredentials;
	private final String corsMaxAge;
	
	public CORSHeaders(
		final String corsAllowOrigin,
		final String corsAllowHeaders,
		final String corsAllowMethods,
		final boolean corsAllowCredentials,
		final int corsMaxAge)
	{
		this(
			configuration()
				.corsAllowOrigin(corsAllowOrigin)
				.corsAllowHeaders(corsAllowHeaders)
				.corsAllowMethods(corsAllowMethods)
				.corsAllowCredentials(corsAllowCredentials)
				.corsMaxAgeInSeconds(corsMaxAge)
		);
	}
	
	public CORSHeaders(final ServerConfiguration configuration)
	{
		this.corsAllowOrigin = configuration.corsAllowOrigin();
		this.corsAllowHeaders = configuration.corsAllowHeaders();
		this.corsAllowMethods = configuration.corsAllowMethods();
		this.corsAllowCredentials = configuration.corsAllowCredentials();
		this.corsMaxAge = "" + configuration.corsMaxAgeInSeconds();
	}
	
	public static boolean isPreflightRequest(final ServerConfiguration configuration, final HttpRequest request)
	{
		final Headers headers = request.getHeaders();
		final boolean isPreflightRequest = request.getMethod().getValue().equals(OPTIONS.name()) &&
			headers.containsEntry(HttpHeaderNames.ORIGIN.toString()) &&
			headers.containsEntry(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD.toString());
		if(isPreflightRequest)
		{
			configuration.enableCORSForAPI(true);
		}
		return isPreflightRequest;
	}
	
	public void addCORSHeaders(final HttpRequest request, final HttpResponse response)
	{
		final String origin = request.getFirstHeader(HttpHeaderNames.ORIGIN.toString());
		if(NULL_ORIGIN.equals(origin))
		{
			this.setHeaderIfNotAlreadyExists(
				response,
				HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(),
				NULL_ORIGIN);
		}
		else if(!origin.isEmpty() && this.corsAllowCredentials)
		{
			this.setHeaderIfNotAlreadyExists(response, HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(), origin);
			this.setHeaderIfNotAlreadyExists(
				response,
				HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(),
				"true");
		}
		else
		{
			this.setHeaderIfNotAlreadyExists(
				response,
				HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString(),
				this.corsAllowOrigin);
			this.setHeaderIfNotAlreadyExists(
				response,
				HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS.toString(),
				"" + this.corsAllowCredentials);
		}
		this.setHeaderIfNotAlreadyExists(
			response,
			HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS.toString(),
			this.corsAllowMethods);
		String allowHeaders = this.corsAllowHeaders;
		if(!request.getFirstHeader(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS.toString()).isEmpty())
		{
			allowHeaders += ", " + request.getFirstHeader(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS.toString());
		}
		this.setHeaderIfNotAlreadyExists(
			response,
			HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS.toString(),
			allowHeaders);
		this.setHeaderIfNotAlreadyExists(
			response,
			HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS.toString(),
			allowHeaders);
		this.setHeaderIfNotAlreadyExists(response, HttpHeaderNames.ACCESS_CONTROL_MAX_AGE.toString(), this.corsMaxAge);
	}
	
	private void setHeaderIfNotAlreadyExists(final HttpResponse response, final String name, final String value)
	{
		if(response.getFirstHeader(name).isEmpty())
		{
			response.withHeader(name, value);
		}
	}
}
