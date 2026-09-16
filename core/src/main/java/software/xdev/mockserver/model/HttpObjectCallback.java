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
package software.xdev.mockserver.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;


public class HttpObjectCallback extends Action<HttpObjectCallback>
{
	private int hashCode;
	private String clientId;
	private Boolean responseCallback;
	private Type actionType;
	
	public String getClientId()
	{
		return this.clientId;
	}
	
	/**
	 * The client id of the web socket client that will handle the callback
	 * <p>
	 * The client id must be for client with an open web socket, if no client is found with id a 404 response will be
	 * returned
	 *
	 * @param clientId client id of the web socket client that will handle the callback
	 */
	public HttpObjectCallback withClientId(final String clientId)
	{
		this.clientId = clientId;
		this.hashCode = 0;
		return this;
	}
	
	public Boolean getResponseCallback()
	{
		return this.responseCallback;
	}
	
	public HttpObjectCallback withResponseCallback(final Boolean responseCallback)
	{
		this.responseCallback = responseCallback;
		this.hashCode = 0;
		return this;
	}
	
	@SuppressWarnings("UnusedReturnValue")
	public HttpObjectCallback withActionType(final Type actionType)
	{
		this.actionType = actionType;
		this.hashCode = 0;
		return this;
	}
	
	@Override
	@JsonIgnore
	public Type getType()
	{
		return this.actionType;
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
		if(!super.equals(o))
		{
			return false;
		}
		final HttpObjectCallback that = (HttpObjectCallback)o;
		return Objects.equals(this.clientId, that.clientId)
			&& Objects.equals(this.responseCallback, that.responseCallback)
			&& this.actionType == that.actionType;
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(super.hashCode(), this.clientId, this.responseCallback, this.actionType);
		}
		return this.hashCode;
	}
}
