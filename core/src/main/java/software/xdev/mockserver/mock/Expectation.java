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
package software.xdev.mockserver.mock;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.matchers.Times;
import software.xdev.mockserver.model.Action;
import software.xdev.mockserver.model.HttpClassCallback;
import software.xdev.mockserver.model.HttpError;
import software.xdev.mockserver.model.HttpForward;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpOverrideForwardedRequest;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.uuid.UUIDService;


@SuppressWarnings({"rawtypes", "PMD.GodClass"})
public class Expectation extends ObjectWithJsonToString
{
	private static final AtomicInteger EXPECTATION_COUNTER = new AtomicInteger(0);
	private static final long START_TIME = System.currentTimeMillis();
	private int hashCode;
	private String id;
	@JsonIgnore
	private long created;
	private int priority;
	private SortableExpectationId sortableExpectationId;
	private final RequestDefinition httpRequest;
	private final Times times;
	private final TimeToLive timeToLive;
	private HttpResponse httpResponse;
	private HttpClassCallback httpResponseClassCallback;
	private HttpObjectCallback httpResponseObjectCallback;
	private HttpForward httpForward;
	private HttpClassCallback httpForwardClassCallback;
	private HttpObjectCallback httpForwardObjectCallback;
	private HttpOverrideForwardedRequest httpOverrideForwardedRequest;
	private HttpError httpError;
	
	/**
	 * Specify the HttpRequest to match against as follows:
	 * <p><pre>
	 *     when(
	 *         request()
	 *             .withMethod("GET")
	 *             .withPath("/some/path")
	 *     ).thenRespond(
	 *         response()
	 *             .withContentType(APPLICATION_JSON_UTF_8)
	 *             .withBody("{\"some\": \"body\"}")
	 *     );
	 * </pre><p>
	 *
	 * @param httpRequest the HttpRequest to match against
	 * @return the Expectation
	 */
	public static Expectation when(final HttpRequest httpRequest)
	{
		return new Expectation(httpRequest);
	}
	
	/**
	 * Specify the HttpRequest to match against with a match priority as follows:
	 * <p><pre>
	 *     when(
	 *         request()
	 *             .withMethod("GET")
	 *             .withPath("/some/path"),
	 *         10
	 *     ).thenRespond(
	 *         response()
	 *             .withContentType(APPLICATION_JSON_UTF_8)
	 *             .withBody("{\"some\": \"body\"}")
	 *     );
	 * </pre><p>
	 *
	 * @param httpRequest the HttpRequest to match against
	 * @param priority    the priority with which this expectation is used to match requests compared to other
	 *                    expectations (high first)
	 * @return the Expectation
	 */
	public static Expectation when(final HttpRequest httpRequest, final int priority)
	{
		return new Expectation(httpRequest, Times.unlimited(), TimeToLive.unlimited(), priority);
	}
	
	/**
	 * Specify the HttpRequest to match against for a limit number of times or time as follows:
	 * <p><pre>
	 *     when(
	 *         request()
	 *             .withMethod("GET")
	 *             .withPath("/some/path"),
	 *         5,
	 *         exactly(TimeUnit.SECONDS, 90)
	 *     ).thenRespond(
	 *         response()
	 *             .withContentType(APPLICATION_JSON_UTF_8)
	 *             .withBody("{\"some\": \"body\"}")
	 *     );
	 * </pre><p>
	 *
	 * @param httpRequest the HttpRequest to match against
	 * @param times       the number of times to use this expectation to match requests
	 * @param timeToLive  the time this expectation should be used to match requests
	 * @return the Expectation
	 */
	public static Expectation when(final HttpRequest httpRequest, final Times times, final TimeToLive timeToLive)
	{
		return new Expectation(httpRequest, times, timeToLive, 0);
	}
	
	/**
	 * Specify the HttpRequest to match against for a limit number of times or time and a match priority as follows:
	 * <p><pre>
	 *     when(
	 *         request()
	 *             .withMethod("GET")
	 *             .withPath("/some/path"),
	 *         5,
	 *         exactly(TimeUnit.SECONDS, 90),
	 *         10
	 *     ).thenRespond(
	 *         response()
	 *             .withContentType(APPLICATION_JSON_UTF_8)
	 *             .withBody("{\"some\": \"body\"}")
	 *     );
	 * </pre><p>
	 *
	 * @param httpRequest the HttpRequest to match against
	 * @param times       the number of times to use this expectation to match requests
	 * @param timeToLive  the time this expectation should be used to match requests
	 * @param priority    the priority with which this expectation is used to match requests compared to other
	 *                    expectations (high first)
	 * @return the Expectation
	 */
	public static Expectation when(
		final HttpRequest httpRequest,
		final Times times,
		final TimeToLive timeToLive,
		final int priority)
	{
		return new Expectation(httpRequest, times, timeToLive, priority);
	}
	
	public Expectation(final RequestDefinition requestDefinition)
	{
		this(requestDefinition, Times.unlimited(), TimeToLive.unlimited(), 0);
	}
	
	public Expectation(
		final RequestDefinition requestDefinition,
		final Times times,
		final TimeToLive timeToLive,
		final int priority)
	{
		// ensure created enforces insertion order by relying on system time, and a counter
		EXPECTATION_COUNTER.compareAndSet(Integer.MAX_VALUE, 0);
		this.created = System.currentTimeMillis() - START_TIME + EXPECTATION_COUNTER.incrementAndGet();
		this.httpRequest = requestDefinition;
		this.times = times;
		this.timeToLive = timeToLive;
		this.priority = priority;
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
	public Expectation withId(final String id)
	{
		this.id = id;
		this.sortableExpectationId = null;
		return this;
	}
	
	public Expectation withIdIfNull(final String id)
	{
		if(this.id == null)
		{
			this.id = id;
			this.sortableExpectationId = null;
		}
		return this;
	}
	
	public String getId()
	{
		if(this.id == null)
		{
			this.withId(UUIDService.getUUID());
		}
		return this.id;
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
	public Expectation withPriority(final int priority)
	{
		this.priority = priority;
		this.sortableExpectationId = null;
		return this;
	}
	
	public int getPriority()
	{
		return this.priority;
	}
	
	public Expectation withCreated(final long created)
	{
		this.created = created;
		this.sortableExpectationId = null;
		this.hashCode = 0;
		return this;
	}
	
	public long getCreated()
	{
		return this.created;
	}
	
	@JsonIgnore
	public SortableExpectationId getSortableId()
	{
		if(this.sortableExpectationId == null)
		{
			this.sortableExpectationId = new SortableExpectationId(this.getId(), this.priority, this.created);
		}
		return this.sortableExpectationId;
	}
	
	public RequestDefinition getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public HttpResponse getHttpResponse()
	{
		return this.httpResponse;
	}
	
	public HttpClassCallback getHttpResponseClassCallback()
	{
		return this.httpResponseClassCallback;
	}
	
	public HttpObjectCallback getHttpResponseObjectCallback()
	{
		return this.httpResponseObjectCallback;
	}
	
	public HttpForward getHttpForward()
	{
		return this.httpForward;
	}
	
	public HttpClassCallback getHttpForwardClassCallback()
	{
		return this.httpForwardClassCallback;
	}
	
	public HttpObjectCallback getHttpForwardObjectCallback()
	{
		return this.httpForwardObjectCallback;
	}
	
	public HttpOverrideForwardedRequest getHttpOverrideForwardedRequest()
	{
		return this.httpOverrideForwardedRequest;
	}
	
	public HttpError getHttpError()
	{
		return this.httpError;
	}
	
	@JsonIgnore
	public Action getAction()
	{
		Action action = null;
		if(this.httpResponse != null)
		{
			action = this.getHttpResponse();
		}
		else if(this.httpResponseClassCallback != null)
		{
			action = this.getHttpResponseClassCallback();
		}
		else if(this.httpResponseObjectCallback != null)
		{
			action = this.getHttpResponseObjectCallback();
		}
		else if(this.httpForward != null)
		{
			action = this.getHttpForward();
		}
		else if(this.httpForwardClassCallback != null)
		{
			action = this.getHttpForwardClassCallback();
		}
		else if(this.httpForwardObjectCallback != null)
		{
			action = this.getHttpForwardObjectCallback();
		}
		else if(this.httpOverrideForwardedRequest != null)
		{
			action = this.getHttpOverrideForwardedRequest();
		}
		else if(this.httpError != null)
		{
			action = this.getHttpError();
		}
		if(action != null)
		{
			action.setExpectationId(this.getId());
		}
		return action;
	}
	
	public Times getTimes()
	{
		return this.times;
	}
	
	public TimeToLive getTimeToLive()
	{
		return this.timeToLive;
	}
	
	public Expectation thenRespond(final HttpResponse httpResponse)
	{
		if(httpResponse != null)
		{
			this.validationErrors("a response", httpResponse.getType());
			this.httpResponse = httpResponse;
			this.hashCode = 0;
		}
		return this;
	}
	
	public Expectation thenRespond(final HttpClassCallback httpClassCallback)
	{
		if(httpClassCallback != null)
		{
			httpClassCallback.withActionType(Action.Type.RESPONSE_CLASS_CALLBACK);
			this.validationErrors("a response class callback", httpClassCallback.getType());
			this.httpResponseClassCallback = httpClassCallback;
			this.hashCode = 0;
		}
		return this;
	}
	
	public Expectation thenRespond(final HttpObjectCallback httpObjectCallback)
	{
		if(httpObjectCallback != null)
		{
			httpObjectCallback.withActionType(Action.Type.RESPONSE_OBJECT_CALLBACK);
			this.validationErrors("a response object callback", httpObjectCallback.getType());
			this.httpResponseObjectCallback = httpObjectCallback;
			this.hashCode = 0;
		}
		return this;
	}
	
	public Expectation thenForward(final HttpForward httpForward)
	{
		if(httpForward != null)
		{
			this.validationErrors("a forward", httpForward.getType());
			this.httpForward = httpForward;
			this.hashCode = 0;
		}
		return this;
	}
	
	public Expectation thenForward(final HttpClassCallback httpClassCallback)
	{
		if(httpClassCallback != null)
		{
			httpClassCallback.withActionType(Action.Type.FORWARD_CLASS_CALLBACK);
			this.validationErrors("a forward class callback", httpClassCallback.getType());
			this.httpForwardClassCallback = httpClassCallback;
			this.hashCode = 0;
		}
		return this;
	}
	
	public Expectation thenForward(final HttpObjectCallback httpObjectCallback)
	{
		if(httpObjectCallback != null)
		{
			httpObjectCallback
				.withActionType(Action.Type.FORWARD_OBJECT_CALLBACK);
			this.validationErrors("a forward object callback", httpObjectCallback.getType());
			this.httpForwardObjectCallback = httpObjectCallback;
			this.hashCode = 0;
		}
		return this;
	}
	
	public Expectation thenForward(final HttpOverrideForwardedRequest httpOverrideForwardedRequest)
	{
		if(httpOverrideForwardedRequest != null)
		{
			this.validationErrors("a forward replace", httpOverrideForwardedRequest.getType());
			this.httpOverrideForwardedRequest = httpOverrideForwardedRequest;
			this.hashCode = 0;
		}
		return this;
	}
	
	public Expectation thenError(final HttpError httpError)
	{
		if(httpError != null)
		{
			this.validationErrors("an error", httpError.getType());
			this.httpError = httpError;
			this.hashCode = 0;
		}
		return this;
	}
	
	@SuppressWarnings({"PMD.CognitiveComplexity", "PMD.NPathComplexity", "PMD.CyclomaticComplexity"})
	private void validationErrors(final String actionDescription, final Action.Type actionType)
	{
		if(actionType != Action.Type.RESPONSE && this.httpResponse != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " once a response has been set");
		}
		if(actionType != Action.Type.RESPONSE_CLASS_CALLBACK && this.httpResponseClassCallback != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " once a class callback has been set");
		}
		if(actionType != Action.Type.RESPONSE_OBJECT_CALLBACK && this.httpResponseObjectCallback != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " once an object callback has been set");
		}
		if(actionType != Action.Type.FORWARD && this.httpForward != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " once a forward has been set");
		}
		if(actionType != Action.Type.FORWARD_CLASS_CALLBACK && this.httpForwardClassCallback != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " once a class callback has been set");
		}
		if(actionType != Action.Type.FORWARD_OBJECT_CALLBACK && this.httpForwardObjectCallback != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " once an object callback has been set");
		}
		if(actionType != Action.Type.FORWARD_REPLACE && this.httpOverrideForwardedRequest != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " once a forward replace has been set");
		}
		if(actionType != Action.Type.ERROR && this.httpError != null)
		{
			throw new IllegalArgumentException(
				"It is not possible to set " + actionDescription + " callback once an error has been set");
		}
	}
	
	@JsonIgnore
	public boolean isActive()
	{
		return this.hasRemainingMatches() && this.isStillAlive();
	}
	
	private boolean hasRemainingMatches()
	{
		return this.times == null || this.times.greaterThenZero();
	}
	
	private boolean isStillAlive()
	{
		return this.timeToLive == null || this.timeToLive.stillAlive();
	}
	
	public boolean decrementRemainingMatches()
	{
		if(this.times != null)
		{
			return this.times.decrement();
		}
		return false;
	}
	
	@SuppressWarnings("PointlessNullCheck")
	public boolean contains(final HttpRequest httpRequest)
	{
		return httpRequest != null && this.httpRequest.equals(httpRequest);
	}
	
	@Override
	@SuppressWarnings({"MethodDoesntCallSuperMethod", "checkstyle:NoClone"})
	public Expectation clone()
	{
		return new Expectation(this.httpRequest, this.times.clone(), this.timeToLive, this.priority)
			.withId(this.id)
			.withCreated(this.created)
			.thenRespond(this.httpResponse)
			.thenRespond(this.httpResponseClassCallback)
			.thenRespond(this.httpResponseObjectCallback)
			.thenForward(this.httpForward)
			.thenForward(this.httpForwardClassCallback)
			.thenForward(this.httpForwardObjectCallback)
			.thenForward(this.httpOverrideForwardedRequest)
			.thenError(this.httpError);
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		if(this.hashCode() != o.hashCode())
		{
			return false;
		}
		final Expectation that = (Expectation)o;
		return Objects.equals(this.priority, that.priority)
			&& Objects.equals(this.httpRequest, that.httpRequest)
			&& Objects.equals(this.times, that.times)
			&& Objects.equals(this.timeToLive, that.timeToLive)
			&& Objects.equals(this.httpResponse, that.httpResponse)
			&& Objects.equals(this.httpResponseClassCallback, that.httpResponseClassCallback)
			&& Objects.equals(this.httpResponseObjectCallback, that.httpResponseObjectCallback)
			&& Objects.equals(this.httpForward, that.httpForward)
			&& Objects.equals(this.httpForwardClassCallback, that.httpForwardClassCallback)
			&& Objects.equals(this.httpForwardObjectCallback, that.httpForwardObjectCallback)
			&& Objects.equals(this.httpOverrideForwardedRequest, that.httpOverrideForwardedRequest)
			&& Objects.equals(this.httpError, that.httpError);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(
				this.priority,
				this.httpRequest,
				this.times,
				this.timeToLive,
				this.httpResponse,
				this.httpResponseClassCallback,
				this.httpResponseObjectCallback,
				this.httpForward,
				this.httpForwardClassCallback,
				this.httpForwardObjectCallback,
				this.httpOverrideForwardedRequest,
				this.httpError);
		}
		return this.hashCode;
	}
}
