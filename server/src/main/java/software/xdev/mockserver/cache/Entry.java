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
package software.xdev.mockserver.cache;

public class Entry<T>
{
	private final long ttlInMillis;
	private final T value;
	private long expiryInMillis;
	
	public Entry(final long ttlInMillis, final long expiryInMillis, final T value)
	{
		this.ttlInMillis = ttlInMillis;
		this.expiryInMillis = expiryInMillis;
		this.value = value;
	}
	
	public long getTtlInMillis()
	{
		return this.ttlInMillis;
	}
	
	public long getExpiryInMillis()
	{
		return this.expiryInMillis;
	}
	
	public Entry<T> updateExpiryInMillis(final long expiryInMillis)
	{
		this.expiryInMillis = expiryInMillis;
		return this;
	}
	
	public T getValue()
	{
		return this.value;
	}
}
