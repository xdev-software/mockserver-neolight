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

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.mock.HttpState.getPort;
import static software.xdev.mockserver.mock.HttpState.setPort;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.httpclient.SocketCommunicationException;
import software.xdev.mockserver.mock.action.http.HttpForwardActionResult;
import software.xdev.mockserver.model.BinaryMessage;
import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.HttpResponse;


public class Scheduler
{
	private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
	private final ServerConfiguration configuration;
	private final ScheduledExecutorService scheduler;
	
	private final boolean synchronous;
	
	public Scheduler(final ServerConfiguration configuration)
	{
		this(configuration, false);
	}
	
	public Scheduler(final ServerConfiguration configuration, final boolean synchronous)
	{
		this.configuration = configuration;
		this.synchronous = synchronous;
		if(!this.synchronous)
		{
			this.scheduler = new ScheduledThreadPoolExecutor(
				configuration.actionHandlerThreadCount(),
				new SchedulerThreadFactory("Scheduler"),
				new ThreadPoolExecutor.CallerRunsPolicy()
			);
		}
		else
		{
			this.scheduler = null;
		}
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	public synchronized void shutdown()
	{
		if(!this.scheduler.isShutdown())
		{
			this.scheduler.shutdown();
			try
			{
				this.scheduler.awaitTermination(500, MILLISECONDS);
			}
			catch(final InterruptedException ignore)
			{
				// ignore interrupted exception
			}
		}
	}
	
	private void run(final Runnable command, final Integer port)
	{
		setPort(port);
		try
		{
			command.run();
		}
		catch(final Exception ex)
		{
			if(LOG.isInfoEnabled())
			{
				LOG.info("Failed to run", ex);
			}
		}
	}
	
	public void schedule(final Runnable command, final boolean synchronous, final Delay... delays)
	{
		final Delay delay = this.addDelays(delays);
		final Integer port = getPort();
		if(this.synchronous || synchronous)
		{
			if(delay != null)
			{
				delay.applyDelay();
			}
			this.run(command, port);
		}
		else
		{
			if(delay != null)
			{
				this.scheduler.schedule(() -> this.run(command, port), delay.getValue(), delay.getTimeUnit());
			}
			else
			{
				this.run(command, port);
			}
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	private Delay addDelays(final Delay... delays)
	{
		if(delays == null || delays.length == 0)
		{
			return null;
		}
		else if(delays.length == 1)
		{
			return delays[0];
		}
		else if(delays.length == 2 && delays[0] == delays[1])
		{
			return delays[0];
		}
		else
		{
			long timeInMilliseconds = 0;
			for(final Delay delay : delays)
			{
				if(delay != null)
				{
					timeInMilliseconds += delay.getTimeUnit().toMillis(delay.getValue());
				}
			}
			return new Delay(MILLISECONDS, timeInMilliseconds);
		}
	}
	
	public void submit(final Runnable command)
	{
		this.submit(command, false);
	}
	
	public void submit(final Runnable command, final boolean synchronous)
	{
		final Integer port = getPort();
		if(this.synchronous || synchronous)
		{
			this.run(command, port);
		}
		else
		{
			this.scheduler.submit(() -> this.run(command, port));
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	public void submit(
		final HttpForwardActionResult future,
		final Runnable command,
		final boolean synchronous,
		final Predicate<Throwable> logException)
	{
		final Integer port = getPort();
		if(future != null)
		{
			if(this.synchronous || synchronous)
			{
				try
				{
					future.getHttpResponse().get(this.configuration.maxSocketTimeoutInMillis(), MILLISECONDS);
				}
				catch(final TimeoutException e)
				{
					future.getHttpResponse().completeExceptionally(
						new SocketCommunicationException(
							"Response was not received after "
								+ this.configuration.maxSocketTimeoutInMillis()
								+ " milliseconds, to make the proxy wait longer please use \"mockserver"
								+ ".maxSocketTimeout\" "
								+ "system property or configuration.maxSocketTimeout(long milliseconds)",
							e.getCause()));
				}
				catch(final InterruptedException | ExecutionException ex)
				{
					future.getHttpResponse().completeExceptionally(ex);
				}
				this.run(command, port);
			}
			else
			{
				future.getHttpResponse().whenCompleteAsync((httpResponse, throwable) -> {
					if(throwable != null && LOG.isInfoEnabled() && logException.test(throwable))
					{
						LOG.warn("", throwable);
					}
					this.run(command, port);
				}, this.scheduler);
			}
		}
	}
	
	public void submit(final CompletableFuture<BinaryMessage> future, final Runnable command,
		final boolean synchronous)
	{
		final Integer port = getPort();
		if(future != null)
		{
			if(this.synchronous || synchronous)
			{
				try
				{
					future.get(this.configuration.maxSocketTimeoutInMillis(), MILLISECONDS);
				}
				catch(final TimeoutException e)
				{
					future.completeExceptionally(new SocketCommunicationException(
						"Response was not received after " + this.configuration.maxSocketTimeoutInMillis()
							+ " milliseconds, to make the proxy wait longer please use \"mockserver.maxSocketTimeout\""
							+ " system property or ConfigurationProperties.maxSocketTimeout(long milliseconds)",
						e.getCause()));
				}
				catch(final InterruptedException | ExecutionException ex)
				{
					future.completeExceptionally(ex);
				}
				this.run(command, port);
			}
			else
			{
				future.whenCompleteAsync((httpResponse, throwable) -> command.run(), this.scheduler);
			}
		}
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	public void submit(
		final HttpForwardActionResult future,
		final BiConsumer<HttpResponse, Throwable> consumer,
		final boolean synchronous)
	{
		if(future != null)
		{
			if(this.synchronous || synchronous)
			{
				HttpResponse httpResponse = null;
				Throwable exception = null;
				try
				{
					httpResponse = future.getHttpResponse().get(
						this.configuration.maxSocketTimeoutInMillis(),
						MILLISECONDS);
				}
				catch(final TimeoutException e)
				{
					exception = new SocketCommunicationException(
						"Response was not received after " + this.configuration.maxSocketTimeoutInMillis()
							+ " milliseconds, to make the proxy wait longer please use \"mockserver.maxSocketTimeout\""
							+ " system property or ConfigurationProperties.maxSocketTimeout(long milliseconds)",
						e.getCause());
				}
				catch(final InterruptedException | ExecutionException ex)
				{
					exception = ex;
				}
				try
				{
					consumer.accept(httpResponse, exception);
				}
				catch(final Exception ex)
				{
					if(LOG.isInfoEnabled())
					{
						LOG.warn("", ex);
					}
				}
			}
			else
			{
				future.getHttpResponse().whenCompleteAsync(consumer, this.scheduler);
			}
		}
	}
}
