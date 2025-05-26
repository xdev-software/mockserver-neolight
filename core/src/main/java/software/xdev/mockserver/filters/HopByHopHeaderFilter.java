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
package software.xdev.mockserver.filters;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import software.xdev.mockserver.model.Header;
import software.xdev.mockserver.model.Headers;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;


public class HopByHopHeaderFilter
{
	private static final List<String> REQUEST_HEADERS_TO_REMOVE = Arrays.asList(
		"proxy-connection",
		"connection",
		"keep-alive",
		"transfer-encoding",
		"te",
		"trailer",
		"proxy-authorization",
		"proxy-authenticate",
		"upgrade"
	);
	
	private static final List<String> RESPONSE_HEADERS_TO_REMOVE = Arrays.asList(
		"proxy-connection",
		"connection",
		"keep-alive",
		"transfer-encoding",
		"content-length",
		"te",
		"trailer",
		"upgrade"
	);
	
	public HttpRequest onRequest(final HttpRequest request)
	{
		if(request == null)
		{
			return null;
		}
		
		final Headers headers = new Headers();
		for(final Header header : request.getHeaderList())
		{
			if(!REQUEST_HEADERS_TO_REMOVE.contains(header.getName().getValue().toLowerCase(Locale.ENGLISH)))
			{
				headers.withEntry(header);
			}
		}
		final HttpRequest clonedRequest = request.clone();
		if(!headers.isEmpty())
		{
			clonedRequest.withHeaders(headers);
		}
		return clonedRequest;
	}
	
	public HttpResponse onResponse(final HttpResponse response)
	{
		if(response == null)
		{
			return null;
		}
		
		final Headers headers = new Headers();
		for(final Header header : response.getHeaderList())
		{
			if(!RESPONSE_HEADERS_TO_REMOVE.contains(header.getName().getValue().toLowerCase(Locale.ENGLISH)))
			{
				headers.withEntry(header);
			}
		}
		final HttpResponse clonedResponse = response.clone();
		if(!headers.isEmpty())
		{
			clonedResponse.withHeaders(headers);
		}
		return clonedResponse;
	}
}
