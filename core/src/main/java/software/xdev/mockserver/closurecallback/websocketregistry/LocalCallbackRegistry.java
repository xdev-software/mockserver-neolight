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
package software.xdev.mockserver.closurecallback.websocketregistry;

import java.util.Collections;
import java.util.Map;

import software.xdev.mockserver.collections.CircularHashMap;
import software.xdev.mockserver.configuration.ConfigurationProperties;
import software.xdev.mockserver.mock.action.ExpectationCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardCallback;
import software.xdev.mockserver.mock.action.ExpectationResponseCallback;


public final class LocalCallbackRegistry
{
	public static boolean enabled = true;
	private static Map<String, ExpectationResponseCallback> responseCallbackRegistry;
	private static Map<String, ExpectationForwardCallback> forwardCallbackRegistry;
	private static Map<String, ExpectationForwardAndResponseCallback> forwardAndResponseCallbackRegistry;
	private static int maxWebSocketExpectations = ConfigurationProperties.maxWebSocketExpectations();
	
	public static void setMaxWebSocketExpectations(final int maxWebSocketExpectations)
	{
		LocalCallbackRegistry.maxWebSocketExpectations = maxWebSocketExpectations;
	}
	
	public static Map<String, ExpectationResponseCallback> responseCallbackRegistry()
	{
		if(responseCallbackRegistry == null)
		{
			responseCallbackRegistry = Collections.synchronizedMap(new CircularHashMap<>(maxWebSocketExpectations));
		}
		return responseCallbackRegistry;
	}
	
	public static Map<String, ExpectationForwardCallback> forwardCallbackRegistry()
	{
		if(forwardCallbackRegistry == null)
		{
			forwardCallbackRegistry = Collections.synchronizedMap(new CircularHashMap<>(maxWebSocketExpectations));
		}
		return forwardCallbackRegistry;
	}
	
	public static Map<String, ExpectationForwardAndResponseCallback> forwardAndResponseCallbackRegistry()
	{
		if(forwardAndResponseCallbackRegistry == null)
		{
			forwardAndResponseCallbackRegistry =
				Collections.synchronizedMap(new CircularHashMap<>(maxWebSocketExpectations));
		}
		return forwardAndResponseCallbackRegistry;
	}
	
	public static void registerCallback(final String clientId, final ExpectationCallback<?> expectationCallback)
	{
		// if not added to local registry then web socket will be used
		if(enabled && expectationCallback != null)
		{
			if(expectationCallback instanceof ExpectationResponseCallback)
			{
				responseCallbackRegistry().put(clientId, (ExpectationResponseCallback)expectationCallback);
			}
			else if(expectationCallback instanceof ExpectationForwardAndResponseCallback)
			{
				forwardAndResponseCallbackRegistry().put(
					clientId,
					(ExpectationForwardAndResponseCallback)expectationCallback);
			}
			else if(expectationCallback instanceof ExpectationForwardCallback)
			{
				forwardCallbackRegistry().put(clientId, (ExpectationForwardCallback)expectationCallback);
			}
		}
	}
	
	public static void unregisterCallback(final String clientId)
	{
		responseCallbackRegistry().remove(clientId);
		forwardAndResponseCallbackRegistry().remove(clientId);
		forwardCallbackRegistry().remove(clientId);
	}
	
	public static boolean responseClientExists(final String clientId)
	{
		return responseCallbackRegistry().containsKey(clientId);
	}
	
	public static boolean forwardClientExists(final String clientId)
	{
		return forwardCallbackRegistry().containsKey(clientId)
			|| forwardAndResponseCallbackRegistry().containsKey(clientId);
	}
	
	public static ExpectationResponseCallback retrieveResponseCallback(final String clientId)
	{
		return responseCallbackRegistry().get(clientId);
	}
	
	public static ExpectationForwardCallback retrieveForwardCallback(final String clientId)
	{
		final ExpectationForwardCallback expectationForwardCallback = forwardCallbackRegistry().get(clientId);
		if(expectationForwardCallback == null)
		{
			return retrieveForwardAndResponseCallback(clientId);
		}
		else
		{
			return expectationForwardCallback;
		}
	}
	
	public static ExpectationForwardAndResponseCallback retrieveForwardAndResponseCallback(final String clientId)
	{
		return forwardAndResponseCallbackRegistry().get(clientId);
	}
	
	private LocalCallbackRegistry()
	{
	}
}
