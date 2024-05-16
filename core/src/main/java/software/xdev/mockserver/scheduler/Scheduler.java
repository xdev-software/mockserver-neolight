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

import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.httpclient.SocketCommunicationException;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.mock.action.http.HttpForwardActionResult;
import software.xdev.mockserver.model.BinaryMessage;
import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.WARN;
import static software.xdev.mockserver.mock.HttpState.getPort;
import static software.xdev.mockserver.mock.HttpState.setPort;

public class Scheduler {
    
    private static final Logger LOG = LoggerFactory.getLogger(Scheduler.class);
    private final Configuration configuration;
    private final ScheduledExecutorService scheduler;

    private final boolean synchronous;

    public static class SchedulerThreadFactory implements ThreadFactory {
        
        private static final AtomicInteger THREAD_INIT_NUMBER = new AtomicInteger(0);
        
        private final String name;
        private final boolean daemon;

        public SchedulerThreadFactory(String name) {
            this.name = name;
            this.daemon = true;
        }

        public SchedulerThreadFactory(String name, boolean daemon) {
            this.name = name;
            this.daemon = daemon;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "MockServer-" + name + THREAD_INIT_NUMBER.get());
            thread.setDaemon(daemon);
            return thread;
        }
    }

    public Scheduler(Configuration configuration) {
        this(configuration, false);
    }

    public Scheduler(Configuration configuration, boolean synchronous) {
        this.configuration = configuration;
        this.synchronous = synchronous;
        if (!this.synchronous) {
            this.scheduler = new ScheduledThreadPoolExecutor(
                configuration.actionHandlerThreadCount(),
                new SchedulerThreadFactory("Scheduler"),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
        } else {
            this.scheduler = null;
        }
    }

    public synchronized void shutdown() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(500, MILLISECONDS);
            } catch (InterruptedException ignore) {
                // ignore interrupted exception
            }
        }
    }

    private void run(Runnable command, Integer port) {
        setPort(port);
        try {
            command.run();
        } catch (Exception ex) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Failed to run", ex);
            }
        }
    }

    public void schedule(Runnable command, boolean synchronous, Delay... delays) {
        Delay delay = addDelays(delays);
        Integer port = getPort();
        if (this.synchronous || synchronous) {
            if (delay != null) {
                delay.applyDelay();
            }
            run(command, port);
        } else {
            if (delay != null) {
                scheduler.schedule(() -> run(command, port), delay.getValue(), delay.getTimeUnit());
            } else {
                run(command, port);
            }
        }
    }

    private Delay addDelays(Delay... delays) {
        if (delays == null || delays.length == 0) {
            return null;
        } else if (delays.length == 1) {
            return delays[0];
        } else if (delays.length == 2 && delays[0] == delays[1]) {
            return delays[0];
        } else {
            long timeInMilliseconds = 0;
            for (Delay delay : delays) {
                if (delay != null) {
                    timeInMilliseconds += delay.getTimeUnit().toMillis(delay.getValue());
                }
            }
            return new Delay(MILLISECONDS, timeInMilliseconds);
        }
    }

    public void submit(Runnable command) {
        submit(command, false);
    }

    public void submit(Runnable command, boolean synchronous) {
        Integer port = getPort();
        if (this.synchronous || synchronous) {
            run(command, port);
        } else {
            scheduler.submit(() -> run(command, port));
        }
    }

    public void submit(HttpForwardActionResult future, Runnable command, boolean synchronous, Predicate<Throwable> logException) {
        Integer port = getPort();
        if (future != null) {
            if (this.synchronous || synchronous) {
                try {
                    future.getHttpResponse().get(configuration.maxSocketTimeoutInMillis(), MILLISECONDS);
                } catch (TimeoutException e) {
                    future.getHttpResponse().completeExceptionally(
                        new SocketCommunicationException("Response was not received after "
                            + configuration.maxSocketTimeoutInMillis()
                            + " milliseconds, to make the proxy wait longer please use \"mockserver.maxSocketTimeout\" "
                            + "system property or configuration.maxSocketTimeout(long milliseconds)",
                            e.getCause()));
                } catch (InterruptedException | ExecutionException ex) {
                    future.getHttpResponse().completeExceptionally(ex);
                }
                run(command, port);
            } else {
                future.getHttpResponse().whenCompleteAsync((httpResponse, throwable) -> {
                    if (throwable != null && LOG.isInfoEnabled() && logException.test(throwable)) {
                        LOG.warn("", throwable);
                    }
                    run(command, port);
                }, scheduler);
            }
        }
    }

    public void submit(CompletableFuture<BinaryMessage> future, Runnable command, boolean synchronous) {
        Integer port = getPort();
        if (future != null) {
            if (this.synchronous || synchronous) {
                try {
                    future.get(configuration.maxSocketTimeoutInMillis(), MILLISECONDS);
                } catch (TimeoutException e) {
                    future.completeExceptionally(new SocketCommunicationException("Response was not received after " + configuration.maxSocketTimeoutInMillis() + " milliseconds, to make the proxy wait longer please use \"mockserver.maxSocketTimeout\" system property or ConfigurationProperties.maxSocketTimeout(long milliseconds)", e.getCause()));
                } catch (InterruptedException | ExecutionException ex) {
                    future.completeExceptionally(ex);
                }
                run(command, port);
            } else {
                future.whenCompleteAsync((httpResponse, throwable) -> command.run(), scheduler);
            }
        }
    }

    public void submit(HttpForwardActionResult future, BiConsumer<HttpResponse, Throwable> consumer, boolean synchronous) {
        if (future != null) {
            if (this.synchronous || synchronous) {
                HttpResponse httpResponse = null;
                Throwable exception = null;
                try {
                    httpResponse = future.getHttpResponse().get(configuration.maxSocketTimeoutInMillis(), MILLISECONDS);
                } catch (TimeoutException e) {
                    exception = new SocketCommunicationException("Response was not received after " + configuration.maxSocketTimeoutInMillis() + " milliseconds, to make the proxy wait longer please use \"mockserver.maxSocketTimeout\" system property or ConfigurationProperties.maxSocketTimeout(long milliseconds)", e.getCause());
                } catch (InterruptedException | ExecutionException ex) {
                    exception = ex;
                }
                try {
                    consumer.accept(httpResponse, exception);
                } catch (Exception ex) {
                    if (LOG.isInfoEnabled()) {
                        LOG.warn("", ex);
                    }
                }
            } else {
                future.getHttpResponse().whenCompleteAsync(consumer, scheduler);
            }
        }
    }

}
