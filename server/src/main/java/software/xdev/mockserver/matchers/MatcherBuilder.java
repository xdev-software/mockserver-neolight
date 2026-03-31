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
package software.xdev.mockserver.matchers;

import static java.util.concurrent.TimeUnit.MINUTES;

import software.xdev.mockserver.cache.LRUCache;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.RequestDefinition;


public class MatcherBuilder
{
	private final ServerConfiguration configuration;
	private final LRUCache<RequestDefinition, HttpRequestMatcher> requestMatcherLRUCache;
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public MatcherBuilder(final ServerConfiguration configuration)
	{
		this.configuration = configuration;
		this.requestMatcherLRUCache = new LRUCache<>(250, MINUTES.toMillis(10));
	}
	
	public HttpRequestMatcher transformsToMatcher(final RequestDefinition requestDefinition)
	{
		HttpRequestMatcher httpRequestMatcher = this.requestMatcherLRUCache.get(requestDefinition);
		if(httpRequestMatcher == null)
		{
			httpRequestMatcher = new HttpRequestPropertiesMatcher(this.configuration);
			httpRequestMatcher.update(requestDefinition);
			this.requestMatcherLRUCache.put(requestDefinition, httpRequestMatcher);
		}
		return httpRequestMatcher;
	}
	
	public HttpRequestMatcher transformsToMatcher(final Expectation expectation)
	{
		final HttpRequestMatcher httpRequestMatcher = new HttpRequestPropertiesMatcher(this.configuration);
		httpRequestMatcher.update(expectation);
		return httpRequestMatcher;
	}
}
