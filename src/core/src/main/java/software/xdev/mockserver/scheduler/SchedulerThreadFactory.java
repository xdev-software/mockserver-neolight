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
package software.xdev.mockserver.scheduler;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


public class SchedulerThreadFactory implements ThreadFactory
{
	private static final AtomicInteger THREAD_INIT_NUMBER = new AtomicInteger(0);
	
	private final String name;
	private final boolean daemon;
	
	public SchedulerThreadFactory(final String name)
	{
		this.name = name;
		this.daemon = true;
	}
	
	public SchedulerThreadFactory(final String name, final boolean daemon)
	{
		this.name = name;
		this.daemon = daemon;
	}
	
	@Override
	public Thread newThread(final Runnable runnable)
	{
		final Thread thread = new Thread(runnable, "MockServer-" + this.name + THREAD_INIT_NUMBER.get());
		thread.setDaemon(this.daemon);
		return thread;
	}
}
