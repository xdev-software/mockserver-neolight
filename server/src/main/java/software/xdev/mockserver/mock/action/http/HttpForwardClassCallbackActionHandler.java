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
package software.xdev.mockserver.mock.action.http;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.httpclient.NettyHttpClient;
import software.xdev.mockserver.mock.action.ExpectationCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardCallback;
import software.xdev.mockserver.model.HttpClassCallback;
import software.xdev.mockserver.model.HttpRequest;


public class HttpForwardClassCallbackActionHandler extends HttpForwardAction
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpForwardClassCallbackActionHandler.class);
	
	public HttpForwardClassCallbackActionHandler(final NettyHttpClient httpClient)
	{
		super(httpClient);
	}
	
	public HttpForwardActionResult handle(final HttpClassCallback httpClassCallback, final HttpRequest request)
	{
		return this.invokeCallbackMethod(httpClassCallback, request);
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	private <T extends ExpectationCallback> T instantiateCallback(
		final HttpClassCallback httpClassCallback,
		final Class<T> callbackClass)
	{
		try
		{
			final Class expectationCallbackClass = Class.forName(httpClassCallback.getCallbackClass());
			if(callbackClass.isAssignableFrom(expectationCallbackClass))
			{
				final Constructor<? extends T> constructor = expectationCallbackClass.getConstructor();
				return constructor.newInstance();
			}
			else
			{
				LOG.warn(
					"{} does not implement {} required for forwarded requests with class callback",
					httpClassCallback.getCallbackClass(),
					callbackClass.getName());
			}
		}
		catch(final ClassNotFoundException e)
		{
			LOG.error(
				"ClassNotFoundException - while trying to instantiate {} class \"{}\"",
				callbackClass.getSimpleName(),
				httpClassCallback.getCallbackClass(),
				e);
		}
		catch(final NoSuchMethodException e)
		{
			LOG.error(
				"NoSuchMethodException - while trying to create default constructor on {} class \"{}\"",
				callbackClass.getSimpleName(),
				httpClassCallback.getCallbackClass(),
				e);
		}
		catch(final InvocationTargetException | InstantiationException | IllegalAccessException e)
		{
			LOG.error(
				"InvocationTargetException - while trying to execute default constructor on {} class \"{}\"",
				callbackClass.getSimpleName(),
				httpClassCallback.getCallbackClass(),
				e);
		}
		return null;
	}
	
	private HttpForwardActionResult invokeCallbackMethod(
		final HttpClassCallback httpClassCallback,
		final HttpRequest httpRequest)
	{
		if(httpRequest != null)
		{
			final ExpectationForwardCallback expectationForwardCallback =
				this.instantiateCallback(httpClassCallback, ExpectationForwardCallback.class);
			final ExpectationForwardAndResponseCallback expectationForwardResponseCallback =
				this.instantiateCallback(httpClassCallback, ExpectationForwardAndResponseCallback.class);
			if(expectationForwardCallback != null || expectationForwardResponseCallback != null)
			{
				try
				{
					final HttpRequest request = expectationForwardCallback != null
						? expectationForwardCallback.handle(httpRequest)
						: httpRequest;
					return this.sendRequest(request, null, response -> {
						try
						{
							return expectationForwardResponseCallback != null
								? expectationForwardResponseCallback.handle(request, response)
								: response;
						}
						catch(final Exception ex)
						{
							LOG.error(
								"{} throw exception while executing handle callback method",
								httpClassCallback.getCallbackClass(),
								ex);
							return response;
						}
					});
				}
				catch(final Exception ex)
				{
					LOG.error(
						"{} throw exception while executing handle callback method",
						httpClassCallback.getCallbackClass(),
						ex);
					return this.notFoundFuture(httpRequest);
				}
			}
			else
			{
				return this.sendRequest(httpRequest, null, null);
			}
		}
		else
		{
			return this.notFoundFuture(null);
		}
	}
}
