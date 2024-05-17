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
package software.xdev.mockserver.event.model;

import static software.xdev.mockserver.model.HttpRequest.request;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lmax.disruptor.EventTranslator;

import software.xdev.mockserver.matchers.HttpRequestMatcher;
import software.xdev.mockserver.matchers.MatchDifference;
import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.matchers.Times;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.HttpError;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.RequestDefinition;
import software.xdev.mockserver.serialization.ObjectMapperFactory;
import software.xdev.mockserver.time.EpochService;
import software.xdev.mockserver.uuid.UUIDService;


public class EventEntry implements EventTranslator<EventEntry>
{
	private static final RequestDefinition[] EMPTY_REQUEST_DEFINITIONS = new RequestDefinition[0];
	private static final RequestDefinition[] DEFAULT_REQUESTS_DEFINITIONS = {request()};
	public static final DateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private int hashCode;
	private String id;
	private String correlationId;
	private long epochTime = EpochService.currentTimeMillis();
	private String timestamp;
	private EventType type;
	private RequestDefinition[] httpRequests;
	private HttpResponse httpResponse;
	private HttpError httpError;
	private Expectation expectation;
	private String expectationId;
	private Exception exception;
	private Runnable consumer;
	private boolean deleted;
	
	public EventEntry()
	{
	}
	
	private EventEntry setId(final String id)
	{
		this.id = id;
		return this;
	}
	
	@JsonIgnore
	public String id()
	{
		if(this.id == null)
		{
			this.id = UUIDService.getUUID();
		}
		return this.id;
	}
	
	public void clear()
	{
		this.id = null;
		this.correlationId = null;
		this.epochTime = -1;
		this.timestamp = null;
		this.type = null;
		this.httpRequests = null;
		this.httpResponse = null;
		this.httpError = null;
		this.expectation = null;
		this.expectationId = null;
		this.exception = null;
		this.consumer = null;
		this.deleted = false;
	}
	
	public long getEpochTime()
	{
		return this.epochTime;
	}
	
	public EventEntry setEpochTime(final long epochTime)
	{
		this.epochTime = epochTime;
		this.timestamp = null;
		return this;
	}
	
	public String getTimestamp()
	{
		if(this.timestamp == null)
		{
			this.timestamp = LOG_DATE_FORMAT.format(new Date(this.epochTime));
		}
		return this.timestamp;
	}
	
	public EventType getType()
	{
		return this.type;
	}
	
	public EventEntry setType(final EventType type)
	{
		this.type = type;
		return this;
	}
	
	public String getCorrelationId()
	{
		return this.correlationId;
	}
	
	public EventEntry setCorrelationId(final String correlationId)
	{
		this.correlationId = correlationId;
		return this;
	}
	
	@JsonIgnore
	public RequestDefinition[] getHttpRequests()
	{
		if(this.httpRequests == null)
		{
			return EMPTY_REQUEST_DEFINITIONS;
		}
		return this.httpRequests;
	}
	
	@JsonIgnore
	public boolean matches(final HttpRequestMatcher matcher)
	{
		if(matcher == null)
		{
			return true;
		}
		if(this.httpRequests == null || this.httpRequests.length == 0)
		{
			return true;
		}
		for(final RequestDefinition httpRequest : this.httpRequests)
		{
			final RequestDefinition request = httpRequest.cloneWithLogCorrelationId();
			if(matcher.matches(
				this.type == EventType.RECEIVED_REQUEST ? new MatchDifference(false, request) : null,
				request))
			{
				return true;
			}
		}
		return false;
	}
	
	public EventEntry setHttpRequests(final RequestDefinition[] httpRequests)
	{
		this.httpRequests = httpRequests;
		return this;
	}
	
	public RequestDefinition getHttpRequest()
	{
		if(this.httpRequests != null && this.httpRequests.length > 0)
		{
			return this.httpRequests[0];
		}
		return null;
	}
	
	public EventEntry setHttpRequest(final RequestDefinition httpRequest)
	{
		if(httpRequest != null)
		{
			if(isNotBlank(httpRequest.getLogCorrelationId()))
			{
				this.setCorrelationId(httpRequest.getLogCorrelationId());
			}
			this.httpRequests = new RequestDefinition[]{httpRequest};
		}
		else
		{
			this.httpRequests = DEFAULT_REQUESTS_DEFINITIONS;
		}
		return this;
	}
	
	public HttpResponse getHttpResponse()
	{
		return this.httpResponse;
	}
	
	public EventEntry setHttpResponse(final HttpResponse httpResponse)
	{
		this.httpResponse = httpResponse;
		return this;
	}
	
	public HttpError getHttpError()
	{
		return this.httpError;
	}
	
	public EventEntry setHttpError(final HttpError httpError)
	{
		this.httpError = httpError;
		return this;
	}
	
	public Expectation getExpectation()
	{
		return this.expectation;
	}
	
	public EventEntry setExpectation(final Expectation expectation)
	{
		this.expectation = expectation;
		return this;
	}
	
	public EventEntry setExpectation(final RequestDefinition httpRequest, final HttpResponse httpResponse)
	{
		this.expectation =
			new Expectation(httpRequest, Times.once(), TimeToLive.unlimited(), 0).thenRespond(httpResponse);
		return this;
	}
	
	public String getExpectationId()
	{
		return this.expectationId;
	}
	
	public EventEntry setExpectationId(final String expectationId)
	{
		this.expectationId = expectationId;
		return this;
	}
	
	public boolean matchesAnyExpectationId(final List<String> expectationIds)
	{
		if(expectationIds != null && isNotBlank(this.expectationId))
		{
			return expectationIds.contains(this.expectationId);
		}
		return false;
	}
	
	public Exception getException()
	{
		return this.exception;
	}
	
	public EventEntry setException(final Exception ex)
	{
		this.exception = ex;
		return this;
	}
	
	public Runnable getConsumer()
	{
		return this.consumer;
	}
	
	public EventEntry setConsumer(final Runnable consumer)
	{
		this.consumer = consumer;
		return this;
	}
	
	public boolean isDeleted()
	{
		return this.deleted;
	}
	
	public EventEntry setDeleted(final boolean deleted)
	{
		this.deleted = deleted;
		return this;
	}
	
	public EventEntry cloneAndClear()
	{
		final EventEntry clone = this.clone();
		this.clear();
		return clone;
	}
	
	@Override
	@SuppressWarnings({"MethodDoesntCallSuperMethod", "checkstyle:NoClone"})
	public EventEntry clone()
	{
		return new EventEntry()
			.setId(this.id())
			.setType(this.getType())
			.setEpochTime(this.getEpochTime())
			.setCorrelationId(this.getCorrelationId())
			.setHttpRequests(this.getHttpRequests())
			.setHttpResponse(this.getHttpResponse())
			.setHttpError(this.getHttpError())
			.setExpectation(this.getExpectation())
			.setExpectationId(this.getExpectationId())
			.setException(this.getException())
			.setConsumer(this.getConsumer())
			.setDeleted(this.isDeleted());
	}
	
	@Override
	public void translateTo(final EventEntry event, final long sequence)
	{
		event
			.setId(this.id())
			.setType(this.getType())
			.setEpochTime(this.getEpochTime())
			.setCorrelationId(this.getCorrelationId())
			.setHttpRequests(this.getHttpRequests())
			.setHttpResponse(this.getHttpResponse())
			.setHttpError(this.getHttpError())
			.setExpectation(this.getExpectation())
			.setExpectationId(this.getExpectationId())
			.setException(this.getException())
			.setConsumer(this.getConsumer())
			.setDeleted(this.isDeleted());
		this.clear();
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
		final EventEntry eventEntry = (EventEntry)o;
		return this.epochTime == eventEntry.epochTime
			&& this.deleted == eventEntry.deleted
			&& this.type == eventEntry.type
			&& Objects.equals(this.httpResponse, eventEntry.httpResponse)
			&& Objects.equals(this.httpError, eventEntry.httpError)
			&& Objects.equals(this.expectation, eventEntry.expectation)
			&& Objects.equals(this.expectationId, eventEntry.expectationId)
			&& Objects.equals(this.consumer, eventEntry.consumer)
			&& Arrays.equals(this.httpRequests, eventEntry.httpRequests);
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			int result =
				Objects.hash(this.epochTime,
					this.deleted, this.type, this.httpResponse, this.httpError,
					this.expectation, this.expectationId, this.consumer);
			result = 31 * result + Arrays.hashCode(this.httpRequests);
			this.hashCode = result;
		}
		return this.hashCode;
	}
	
	@Override
	public String toString()
	{
		try
		{
			return ObjectMapperFactory
				.createObjectMapper(true, false)
				.writeValueAsString(this);
		}
		catch(final Exception e)
		{
			return super.toString();
		}
	}
	
	public enum EventType
	{
		RUNNABLE,
		RETRIEVED,
		RECEIVED_REQUEST,
		EXPECTATION_RESPONSE,
		NO_MATCH_RESPONSE,
		FORWARDED_REQUEST,
	}
}
