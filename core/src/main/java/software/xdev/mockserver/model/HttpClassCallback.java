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

import software.xdev.mockserver.mock.action.ExpectationCallback;


@SuppressWarnings("UnusedReturnValue")
public class HttpClassCallback extends Action<HttpClassCallback>
{
	private int hashCode;
	private String callbackClass;
	private Type actionType;
	
	/**
	 * Static builder to create a callback.
	 */
	public static HttpClassCallback callback()
	{
		return new HttpClassCallback();
	}
	
	/**
	 * Static builder to create a callback, which take a callback class as a string.
	 * <p>
	 * The callback class must: - implement software.xdev.mockserver.mock.action.ExpectationResponseCallback or -
	 * implement software.xdev.mockserver.mock.action.ExpectationForwardCallback or - implement
	 * software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback - have a zero argument constructor -
	 * be available in the classpath of the MockServer
	 *
	 * @param callbackClass class to callback as a fully qualified class name, i.e.
	 *                      "com.foo.MyExpectationResponseCallback"
	 */
	public static HttpClassCallback callback(final String callbackClass)
	{
		return new HttpClassCallback().withCallbackClass(callbackClass);
	}
	
	/**
	 * Static builder to create a callback, which take a callback class.
	 * <p>
	 * The callback class must: - implement software.xdev.mockserver.mock.action.ExpectationResponseCallback or -
	 * implement software.xdev.mockserver.mock.action.ExpectationForwardCallback or - implement
	 * software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback - have a zero argument constructor -
	 * be available in the classpath of the MockServer
	 *
	 * @param callbackClass class to callback as a fully qualified class name, i.e.
	 *                      "com.foo.MyExpectationResponseCallback"
	 */
	@SuppressWarnings("rawtypes")
	public static HttpClassCallback callback(final Class<? extends ExpectationCallback<? extends HttpMessage>> callbackClass)
	{
		return new HttpClassCallback().withCallbackClass(callbackClass);
	}
	
	public String getCallbackClass()
	{
		return this.callbackClass;
	}
	
	/**
	 * The class to callback as a fully qualified class name
	 * <p>
	 * The callback class must: - implement software.xdev.mockserver.mock.action.ExpectationResponseCallback or -
	 * implement software.xdev.mockserver.mock.action.ExpectationForwardCallback or - implement
	 * software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback - have a zero argument constructor -
	 * be available in the classpath of the MockServer
	 *
	 * @param callbackClass class to callback as a fully qualified class name, i.e.
	 *                      "com.foo.MyExpectationResponseCallback"
	 */
	public HttpClassCallback withCallbackClass(final String callbackClass)
	{
		this.callbackClass = callbackClass;
		this.hashCode = 0;
		return this;
	}
	
	/**
	 * The class to callback
	 * <p>
	 * The callback class must: - implement software.xdev.mockserver.mock.action.ExpectationResponseCallback or -
	 * implement software.xdev.mockserver.mock.action.ExpectationForwardCallback or - implement
	 * software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback - have a zero argument constructor -
	 * be available in the classpath of the MockServer
	 *
	 * @param callbackClass class to callback as a fully qualified class name, i.e.
	 *                      "com.foo.MyExpectationResponseCallback"
	 */
	@SuppressWarnings("rawtypes")
	public HttpClassCallback withCallbackClass(final Class<? extends ExpectationCallback<? extends HttpMessage>> callbackClass)
	{
		this.callbackClass = callbackClass.getName();
		this.hashCode = 0;
		return this;
	}
	
	public HttpClassCallback withActionType(final Type actionType)
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
		final HttpClassCallback that = (HttpClassCallback)o;
		return Objects.equals(this.callbackClass, that.callbackClass) &&
			this.actionType == that.actionType;
	}
	
	@Override
	public int hashCode()
	{
		if(this.hashCode == 0)
		{
			this.hashCode = Objects.hash(super.hashCode(), this.callbackClass, this.actionType);
		}
		return this.hashCode;
	}
}
