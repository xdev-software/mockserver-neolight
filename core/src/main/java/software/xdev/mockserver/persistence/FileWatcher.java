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
package software.xdev.mockserver.persistence;

import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.scheduler.Scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.slf4j.event.Level.INFO;

public class FileWatcher {

    private static ScheduledExecutorService scheduler;

    public synchronized static ScheduledExecutorService getScheduler() {
        if (scheduler == null) {
            scheduler = new ScheduledThreadPoolExecutor(
                2,
                new Scheduler.SchedulerThreadFactory("FileWatcher"),
                new ThreadPoolExecutor.CallerRunsPolicy()
            );
            Runtime.getRuntime().addShutdownHook(new Thread(() -> scheduler.shutdown()));
        }
        return scheduler;
    }

    private boolean running = true;
    private final ScheduledFuture<?> scheduledFuture;
    private static long pollPeriod = 5;
    private static TimeUnit pollPeriodUnits = TimeUnit.SECONDS;

    public FileWatcher(Path filePath, Runnable updatedHandler, Consumer<Throwable> errorHandler, MockServerLogger mockServerLogger) {
        final Path path = filePath.getParent() != null ? filePath : Paths.get(new File(".").getAbsolutePath(), filePath.toString());
        final AtomicReference<Integer> fileHash = new AtomicReference<>(getFileHash(path));
        mockServerLogger.logEvent(
            new LogEntry()
                .setLogLevel(INFO)
                .setMessageFormat("watching file:{}with file fingerprint:{}")
                .setArguments(path, fileHash)
        );
        scheduledFuture = getScheduler().scheduleAtFixedRate(() -> {
            try {
                if (!getFileHash(path).equals(fileHash.get())) {
                    updatedHandler.run();
                    fileHash.set(getFileHash(path));
                }
            } catch (Throwable throwable) {
                errorHandler.accept(throwable);
            }
        }, pollPeriod, pollPeriod, pollPeriodUnits);
    }

    private Integer getFileHash(Path path) {
        try {
            return Arrays.hashCode(Files.readAllBytes(path));
        } catch (IOException ioe) {
            return 0;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public FileWatcher setRunning(boolean running) {
        this.running = running;
        if (!running && this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
        return this;
    }

    public static long getPollPeriod() {
        return FileWatcher.pollPeriod;
    }

    public static void setPollPeriod(long pollPeriod) {
        FileWatcher.pollPeriod = pollPeriod;
    }

    public static TimeUnit getPollPeriodUnits() {
        return FileWatcher.pollPeriodUnits;
    }

    public static void setPollPeriodUnits(TimeUnit pollPeriodUnits) {
        FileWatcher.pollPeriodUnits = pollPeriodUnits;
    }
}
