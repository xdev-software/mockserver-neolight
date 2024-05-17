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
package software.xdev.mockserver.serialization.model;

import java.util.Objects;

import software.xdev.mockserver.matchers.TimeToLive;
import software.xdev.mockserver.matchers.Times;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.model.HttpClassCallback;
import software.xdev.mockserver.model.HttpError;
import software.xdev.mockserver.model.HttpForward;
import software.xdev.mockserver.model.HttpObjectCallback;
import software.xdev.mockserver.model.HttpOverrideForwardedRequest;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.ObjectWithJsonToString;
import software.xdev.mockserver.model.RequestDefinition;


public class ExpectationDTO extends ObjectWithJsonToString implements DTO<Expectation>
{
	private String id;
	private Integer priority;
	private RequestDefinitionDTO httpRequest;
	private HttpResponseDTO httpResponse;
	private HttpClassCallbackDTO httpResponseClassCallback;
	private HttpObjectCallbackDTO httpResponseObjectCallback;
	private HttpForwardDTO httpForward;
	private HttpClassCallbackDTO httpForwardClassCallback;
	private HttpObjectCallbackDTO httpForwardObjectCallback;
	private HttpOverrideForwardedRequestDTO httpOverrideForwardedRequest;
	private HttpErrorDTO httpError;
	private software.xdev.mockserver.serialization.model.TimesDTO times;
	private TimeToLiveDTO timeToLive;
	
	public ExpectationDTO(final Expectation expectation)
	{
		if(expectation != null)
		{
			this.id = expectation.getId();
			final Integer priority = expectation.getPriority();
			if(priority != null)
			{
				this.priority = expectation.getPriority();
			}
			final RequestDefinition requestMatcher = expectation.getHttpRequest();
			if(requestMatcher instanceof HttpRequest)
			{
				this.httpRequest = new HttpRequestDTO((HttpRequest)requestMatcher);
			}
			final HttpResponse httpResponse = expectation.getHttpResponse();
			if(httpResponse != null)
			{
				this.httpResponse = new HttpResponseDTO(httpResponse);
			}
			final HttpClassCallback httpResponseClassCallback = expectation.getHttpResponseClassCallback();
			if(httpResponseClassCallback != null)
			{
				this.httpResponseClassCallback = new HttpClassCallbackDTO(httpResponseClassCallback);
			}
			final HttpObjectCallback httpResponseObjectCallback = expectation.getHttpResponseObjectCallback();
			if(httpResponseObjectCallback != null)
			{
				this.httpResponseObjectCallback = new HttpObjectCallbackDTO(httpResponseObjectCallback);
			}
			final HttpForward httpForward = expectation.getHttpForward();
			if(httpForward != null)
			{
				this.httpForward = new HttpForwardDTO(httpForward);
			}
			final HttpClassCallback httpForwardClassCallback = expectation.getHttpForwardClassCallback();
			if(httpForwardClassCallback != null)
			{
				this.httpForwardClassCallback = new HttpClassCallbackDTO(httpForwardClassCallback);
			}
			final HttpObjectCallback httpForwardObjectCallback = expectation.getHttpForwardObjectCallback();
			if(httpForwardObjectCallback != null)
			{
				this.httpForwardObjectCallback = new HttpObjectCallbackDTO(httpForwardObjectCallback);
			}
			final HttpOverrideForwardedRequest httpOverrideForwardedRequest =
				expectation.getHttpOverrideForwardedRequest();
			if(httpOverrideForwardedRequest != null)
			{
				this.httpOverrideForwardedRequest = new HttpOverrideForwardedRequestDTO(httpOverrideForwardedRequest);
			}
			final HttpError httpError = expectation.getHttpError();
			if(httpError != null)
			{
				this.httpError = new HttpErrorDTO(httpError);
			}
			final Times times = expectation.getTimes();
			if(times != null)
			{
				this.times = new software.xdev.mockserver.serialization.model.TimesDTO(times);
			}
			final TimeToLive timeToLive = expectation.getTimeToLive();
			if(timeToLive != null)
			{
				this.timeToLive = new TimeToLiveDTO(timeToLive);
			}
		}
	}
	
	public ExpectationDTO()
	{
	}
	
	@Override
	public Expectation buildObject()
	{
		RequestDefinition httpRequest = null;
		HttpResponse httpResponse = null;
		HttpClassCallback httpResponseClassCallback = null;
		HttpObjectCallback httpResponseObjectCallback = null;
		HttpForward httpForward = null;
		HttpClassCallback httpForwardClassCallback = null;
		HttpObjectCallback httpForwardObjectCallback = null;
		HttpOverrideForwardedRequest httpOverrideForwardedRequest = null;
		HttpError httpError = null;
		final Times times;
		final TimeToLive timeToLive;
		final int priority;
		if(this.httpRequest != null)
		{
			httpRequest = this.httpRequest.buildObject();
		}
		if(this.httpResponse != null)
		{
			httpResponse = this.httpResponse.buildObject();
		}
		if(this.httpResponseClassCallback != null)
		{
			httpResponseClassCallback = this.httpResponseClassCallback.buildObject();
		}
		if(this.httpResponseObjectCallback != null)
		{
			httpResponseObjectCallback = this.httpResponseObjectCallback.buildObject();
		}
		if(this.httpForward != null)
		{
			httpForward = this.httpForward.buildObject();
		}
		if(this.httpForwardClassCallback != null)
		{
			httpForwardClassCallback = this.httpForwardClassCallback.buildObject();
		}
		if(this.httpForwardObjectCallback != null)
		{
			httpForwardObjectCallback = this.httpForwardObjectCallback.buildObject();
		}
		if(this.httpOverrideForwardedRequest != null)
		{
			httpOverrideForwardedRequest = this.httpOverrideForwardedRequest.buildObject();
		}
		if(this.httpError != null)
		{
			httpError = this.httpError.buildObject();
		}
		if(this.times != null)
		{
			times = this.times.buildObject();
		}
		else
		{
			times = Times.unlimited();
		}
		if(this.timeToLive != null)
		{
			timeToLive = this.timeToLive.buildObject();
		}
		else
		{
			timeToLive = TimeToLive.unlimited();
		}
		if(this.priority != null)
		{
			priority = this.priority;
		}
		else
		{
			priority = 0;
		}
		return new Expectation(httpRequest, times, timeToLive, priority)
			.withId(this.id)
			.thenRespond(httpResponse)
			.thenRespond(httpResponseClassCallback)
			.thenRespond(httpResponseObjectCallback)
			.thenForward(httpForward)
			.thenForward(httpForwardClassCallback)
			.thenForward(httpForwardObjectCallback)
			.thenForward(httpOverrideForwardedRequest)
			.thenError(httpError);
	}
	
	public String getId()
	{
		return this.id;
	}
	
	public ExpectationDTO setId(final String id)
	{
		this.id = id;
		return this;
	}
	
	public Integer getPriority()
	{
		return this.priority;
	}
	
	public ExpectationDTO setPriority(final Integer priority)
	{
		this.priority = priority;
		return this;
	}
	
	public RequestDefinitionDTO getHttpRequest()
	{
		return this.httpRequest;
	}
	
	public ExpectationDTO setHttpRequest(final RequestDefinitionDTO httpRequest)
	{
		this.httpRequest = httpRequest;
		return this;
	}
	
	public HttpResponseDTO getHttpResponse()
	{
		return this.httpResponse;
	}
	
	public ExpectationDTO setHttpResponse(final HttpResponseDTO httpResponse)
	{
		this.httpResponse = httpResponse;
		return this;
	}
	
	public HttpClassCallbackDTO getHttpResponseClassCallback()
	{
		return this.httpResponseClassCallback;
	}
	
	public ExpectationDTO setHttpResponseClassCallback(final HttpClassCallbackDTO httpObjectCallback)
	{
		this.httpResponseClassCallback = httpObjectCallback;
		return this;
	}
	
	public HttpObjectCallbackDTO getHttpResponseObjectCallback()
	{
		return this.httpResponseObjectCallback;
	}
	
	public ExpectationDTO setHttpResponseObjectCallback(final HttpObjectCallbackDTO httpObjectCallback)
	{
		this.httpResponseObjectCallback = httpObjectCallback;
		return this;
	}
	
	public HttpForwardDTO getHttpForward()
	{
		return this.httpForward;
	}
	
	public ExpectationDTO setHttpForward(final HttpForwardDTO httpForward)
	{
		this.httpForward = httpForward;
		return this;
	}
	
	public HttpClassCallbackDTO getHttpForwardClassCallback()
	{
		return this.httpForwardClassCallback;
	}
	
	public ExpectationDTO setHttpForwardClassCallback(final HttpClassCallbackDTO httpClassCallback)
	{
		this.httpForwardClassCallback = httpClassCallback;
		return this;
	}
	
	public HttpObjectCallbackDTO getHttpForwardObjectCallback()
	{
		return this.httpForwardObjectCallback;
	}
	
	public ExpectationDTO setHttpForwardObjectCallback(final HttpObjectCallbackDTO httpObjectCallback)
	{
		this.httpForwardObjectCallback = httpObjectCallback;
		return this;
	}
	
	public HttpOverrideForwardedRequestDTO getHttpOverrideForwardedRequest()
	{
		return this.httpOverrideForwardedRequest;
	}
	
	public ExpectationDTO setHttpOverrideForwardedRequest(
		final HttpOverrideForwardedRequestDTO httpOverrideForwardedRequest)
	{
		this.httpOverrideForwardedRequest = httpOverrideForwardedRequest;
		return this;
	}
	
	public HttpErrorDTO getHttpError()
	{
		return this.httpError;
	}
	
	public ExpectationDTO setHttpError(final HttpErrorDTO httpError)
	{
		this.httpError = httpError;
		return this;
	}
	
	public software.xdev.mockserver.serialization.model.TimesDTO getTimes()
	{
		return this.times;
	}
	
	public ExpectationDTO setTimes(final software.xdev.mockserver.serialization.model.TimesDTO times)
	{
		this.times = times;
		return this;
	}
	
	public TimeToLiveDTO getTimeToLive()
	{
		return this.timeToLive;
	}
	
	public ExpectationDTO setTimeToLive(final TimeToLiveDTO timeToLive)
	{
		this.timeToLive = timeToLive;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final ExpectationDTO that))
		{
			return false;
		}
		return Objects.equals(this.getPriority(), that.getPriority())
			&& Objects.equals(this.getHttpRequest(), that.getHttpRequest())
			&& Objects.equals(this.getHttpResponse(), that.getHttpResponse())
			&& Objects.equals(this.getHttpResponseClassCallback(), that.getHttpResponseClassCallback())
			&& Objects.equals(this.getHttpResponseObjectCallback(), that.getHttpResponseObjectCallback())
			&& Objects.equals(this.getHttpForward(), that.getHttpForward())
			&& Objects.equals(this.getHttpForwardClassCallback(), that.getHttpForwardClassCallback())
			&& Objects.equals(this.getHttpForwardObjectCallback(), that.getHttpForwardObjectCallback())
			&& Objects.equals(this.getHttpOverrideForwardedRequest(), that.getHttpOverrideForwardedRequest())
			&& Objects.equals(this.getHttpError(), that.getHttpError())
			&& Objects.equals(this.getTimes(), that.getTimes())
			&& Objects.equals(this.getTimeToLive(), that.getTimeToLive());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.getPriority(),
			this.getHttpRequest(),
			this.getHttpResponse(),
			this.getHttpResponseClassCallback(),
			this.getHttpResponseObjectCallback(),
			this.getHttpForward(),
			this.getHttpForwardClassCallback(),
			this.getHttpForwardObjectCallback(),
			this.getHttpOverrideForwardedRequest(),
			this.getHttpError(),
			this.getTimes(),
			this.getTimeToLive());
	}
}
