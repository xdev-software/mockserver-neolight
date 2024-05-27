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
package software.xdev.mockserver.client;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.concurrent.Future;

import io.netty.channel.nio.NioEventLoopGroup;
import software.xdev.mockserver.client.MockServerClientEventBus.EventType;
import software.xdev.mockserver.closurecallback.websocketclient.WebSocketClient;
import software.xdev.mockserver.closurecallback.websocketclient.WebSocketException;
import software.xdev.mockserver.closurecallback.websocketregistry.LocalCallbackRegistry;
import software.xdev.mockserver.configuration.ClientConfiguration;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.mock.action.ExpectationCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardCallback;
import software.xdev.mockserver.mock.action.ExpectationResponseCallback;
import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.HttpClassCallback;
import software.xdev.mockserver.model.HttpError;
import software.xdev.mockserver.model.HttpForward;
import software.xdev.mockserver.model.HttpMessage;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpOverrideForwardedRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.scheduler.SchedulerThreadFactory;
import software.xdev.mockserver.uuid.UUIDService;


public class ForwardChainExpectation
{
	private final ClientConfiguration configuration;
	private final MockServerClient mockServerClient;
	private final Expectation expectation;
	private final MockServerClientEventBus mockServerEventBus;
	
	ForwardChainExpectation(
		final ClientConfiguration configuration,
		final MockServerClientEventBus mockServerEventBus,
		final MockServerClient mockServerClient,
		final Expectation expectation)
	{
		this.configuration = configuration;
		this.mockServerEventBus = mockServerEventBus;
		this.mockServerClient = mockServerClient;
		this.expectation = expectation;
	}
	
	/**
	 * <p>
	 * Set id of this expectation which can be used to update this expectation later or for clearing or verifying by
	 * expectation id.
	 * </p>
	 * <p>
	 * Note: Each unique expectation must have a unique id otherwise this expectation will update a existing
	 * expectation
	 * with the same id.
	 * </p>
	 *
	 * @param id unique string for expectation's id
	 */
	public ForwardChainExpectation withId(final String id)
	{
		this.expectation.withId(id);
		return this;
	}
	
	/**
	 * <p>
	 * Set priority of this expectation which is used to determine the matching order of expectations when a request is
	 * received.
	 * </p>
	 * <p>
	 * Matching is ordered by priority (highest first) then creation (earliest first).
	 * </p>
	 *
	 * @param priority expectation's priority
	 */
	public ForwardChainExpectation withPriority(final int priority)
	{
		this.expectation.withPriority(priority);
		return this;
	}
	
	/**
	 * Return response when expectation is matched
	 *
	 * @param httpResponse response to return
	 * @return added or updated expectations
	 */
	public Expectation[] respond(final HttpResponse httpResponse)
	{
		this.expectation.thenRespond(httpResponse);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on local class in same JVM implementing ExpectationResponseCallback to generate response to return
	 * when expectation is matched
	 * <p>
	 * The callback class must: - implement software.xdev.mockserver.mock.action.ExpectationResponseCallback - have a
	 * zero argument constructor - be available in the classpath of the MockServer
	 *
	 * @param httpClassCallback class to callback as a fully qualified class name, i.e.
	 *                          "com.foo.MyExpectationResponseCallback"
	 * @return added or updated expectations
	 */
	public Expectation[] respond(final HttpClassCallback httpClassCallback)
	{
		this.expectation.thenRespond(httpClassCallback);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on object locally or remotely (over web socket) to generate response to return when expectation is
	 * matched
	 *
	 * @param expectationResponseCallback object to call locally or remotely to generate response
	 * @return added or updated expectations
	 */
	public Expectation[] respond(final ExpectationResponseCallback expectationResponseCallback)
	{
		this.expectation.thenRespond(new HttpObjectCallback().withClientId(this.registerWebSocketClient(
			expectationResponseCallback,
			null)));
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on object locally or remotely (over web socket) to generate response to return when expectation is
	 * matched
	 *
	 * @param expectationResponseCallback object to call locally or remotely to generate response
	 * @return added or updated expectations
	 */
	public Expectation[] respond(final ExpectationResponseCallback expectationResponseCallback, final Delay delay)
	{
		this.expectation
			.thenRespond(
				new HttpObjectCallback()
					.withClientId(this.registerWebSocketClient(expectationResponseCallback, null))
					.withDelay(delay)
			);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Forward request to the specified host and port when expectation is matched
	 *
	 * @param httpForward host and port to forward to
	 * @return added or updated expectations
	 */
	public Expectation[] forward(final HttpForward httpForward)
	{
		this.expectation.thenForward(httpForward);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on local class in same JVM implementing ExpectationResponseCallback to generate request to forward
	 * when expectation is matched
	 * <p>
	 * The callback class must: - implement software.xdev.mockserver.mock.action.ExpectationForwardCallback or
	 * software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback - have a zero argument constructor -
	 * be available in the classpath of the MockServer
	 *
	 * @param httpClassCallback class to callback as a fully qualified class name, i.e.
	 *                          "com.foo.MyExpectationResponseCallback"
	 * @return added or updated expectations
	 */
	public Expectation[] forward(final HttpClassCallback httpClassCallback)
	{
		this.expectation.thenForward(httpClassCallback);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on object locally or remotely (over web socket) to generate request to forward when expectation is
	 * matched
	 *
	 * @param expectationForwardCallback object to call locally or remotely to generate request
	 * @return added or updated expectations
	 */
	public Expectation[] forward(final ExpectationForwardCallback expectationForwardCallback)
	{
		this.expectation
			.thenForward(
				new HttpObjectCallback()
					.withClientId(this.registerWebSocketClient(expectationForwardCallback, null))
			);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on object locally or remotely (over web socket) to generate request to forward when expectation is
	 * matched
	 *
	 * @param expectationForwardCallback object to call locally or remotely to generate request
	 * @return added or updated expectations
	 */
	public Expectation[] forward(
		final ExpectationForwardCallback expectationForwardCallback,
		final ExpectationForwardAndResponseCallback expectationForwardResponseCallback)
	{
		this.expectation
			.thenForward(
				new HttpObjectCallback()
					.withResponseCallback(true)
					.withClientId(this.registerWebSocketClient(
						expectationForwardCallback,
						expectationForwardResponseCallback))
			);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on object locally or remotely (over web socket) to generate request to forward when expectation is
	 * matched
	 *
	 * @param expectationForwardCallback object to call locally or remotely to generate request
	 * @return added or updated expectations
	 */
	public Expectation[] forward(final ExpectationForwardCallback expectationForwardCallback, final Delay delay)
	{
		this.expectation
			.thenForward(
				new HttpObjectCallback()
					.withClientId(this.registerWebSocketClient(expectationForwardCallback, null))
					.withDelay(delay)
			);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Call method on object locally or remotely (over web socket) to generate request to forward when expectation is
	 * matched
	 *
	 * @param expectationForwardCallback object to call locally or remotely to generate request
	 * @return added or updated expectations
	 */
	public Expectation[] forward(
		final ExpectationForwardCallback expectationForwardCallback,
		final ExpectationForwardAndResponseCallback expectationForwardResponseCallback,
		final Delay delay)
	{
		this.expectation
			.thenForward(
				new HttpObjectCallback()
					.withResponseCallback(true)
					.withClientId(this.registerWebSocketClient(
						expectationForwardCallback,
						expectationForwardResponseCallback))
					.withDelay(delay)
			);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Override fields, headers, and cookies etc in request being forwarded with specified fields, headers and cookies,
	 * etc in the specified request when expectation is matched
	 *
	 * @param httpOverrideForwardedRequest contains request to override request being forwarded
	 * @return added or updated expectations
	 */
	public Expectation[] forward(final HttpOverrideForwardedRequest httpOverrideForwardedRequest)
	{
		this.expectation.thenForward(httpOverrideForwardedRequest);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	/**
	 * Return error when expectation is matched
	 *
	 * @param httpError error to return
	 * @return added or updated expectations
	 */
	public Expectation[] error(final HttpError httpError)
	{
		this.expectation.thenError(httpError);
		return this.mockServerClient.upsert(this.expectation);
	}
	
	@SuppressWarnings("rawtypes")
	private <T extends HttpMessage> String registerWebSocketClient(
		final ExpectationCallback<T> expectationCallback,
		final ExpectationForwardAndResponseCallback expectationForwardResponseCallback)
	{
		try
		{
			final String clientId = UUIDService.getUUID();
			LocalCallbackRegistry.registerCallback(clientId, expectationCallback);
			LocalCallbackRegistry.registerCallback(clientId, expectationForwardResponseCallback);
			final WebSocketClient<T> webSocketClient = new WebSocketClient<>(
				new NioEventLoopGroup(
					this.configuration.webSocketClientEventLoopThreadCount(),
					new SchedulerThreadFactory(WebSocketClient.class.getSimpleName() + "-eventLoop")),
				clientId
			);
			final Future<String> register = webSocketClient.registerExpectationCallback(
				expectationCallback,
				expectationForwardResponseCallback,
				this.mockServerClient.remoteAddress(),
				this.mockServerClient.contextPath()
			);
			this.mockServerEventBus.subscribe(webSocketClient::stopClient, EventType.STOP, EventType.RESET);
			return register.get(this.configuration.maxFutureTimeoutInMillis(), MILLISECONDS);
		}
		catch(final Exception e)
		{
			if(e.getCause() instanceof WebSocketException)
			{
				throw new ClientException(e.getCause().getMessage(), e);
			}
			else
			{
				throw new ClientException("Unable to retrieve client registration id", e);
			}
		}
	}
	
	Expectation getExpectation()
	{
		return this.expectation;
	}
}
