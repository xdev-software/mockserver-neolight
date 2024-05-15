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

import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.RequestMatchers;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier.Cause;
import software.xdev.mockserver.serialization.ExpectationSerializer;
import software.xdev.mockserver.server.initialize.ExpectationInitializerLoader;
import org.slf4j.event.Level;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.event.Level.*;

public class ExpectationFileWatcher {

    private final Configuration configuration;
    private final ExpectationInitializerLoader expectationInitializerLoader;
    private final MockServerLogger mockServerLogger;
    private final RequestMatchers requestMatchers;
    private final ExpectationSerializer expectationSerializer;
    private List<FileWatcher> fileWatchers;

    public ExpectationFileWatcher(Configuration configuration, MockServerLogger mockServerLogger, RequestMatchers requestMatchers, ExpectationInitializerLoader expectationInitializerLoader) {
        this.configuration = configuration;
        if (configuration.watchInitializationJson()) {
            this.expectationSerializer = new ExpectationSerializer(mockServerLogger);
            this.mockServerLogger = mockServerLogger;
            this.requestMatchers = requestMatchers;
            this.expectationInitializerLoader = expectationInitializerLoader;
            List<String> initializationJsonPaths = ExpectationInitializerLoader.expandedInitializationJsonPaths(configuration.initializationJsonPath());
            try {
                fileWatchers = initializationJsonPaths
                    .stream()
                    .map(initializationJsonPath -> {
                        try {
                            return new FileWatcher(Paths.get(initializationJsonPath), () -> {
                                if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                                    mockServerLogger.logEvent(
                                        new LogEntry()
                                            .setLogLevel(DEBUG)
                                            .setMessageFormat("expectation file watcher updating expectations as modification detected on file{}")
                                            .setArguments(configuration.initializationJsonPath())
                                    );
                                }
                                addExpectationsFromInitializer();
                            }, throwable -> {
                                if (MockServerLogger.isEnabled(WARN) && mockServerLogger != null) {
                                    mockServerLogger.logEvent(
                                        new LogEntry()
                                            .setLogLevel(WARN)
                                            .setMessageFormat("exception while processing expectation file update " + throwable.getMessage())
                                            .setThrowable(throwable)
                                    );
                                }
                            }, mockServerLogger);
                        } catch (Throwable throwable) {
                            mockServerLogger.logEvent(
                                new LogEntry()
                                    .setLogLevel(Level.ERROR)
                                    .setMessageFormat("exception creating file watcher for{}")
                                    .setArguments(initializationJsonPath)
                                    .setThrowable(throwable)
                            );
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            } catch (Throwable throwable) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception creating file watchers for{}")
                        .setArguments(initializationJsonPaths)
                        .setThrowable(throwable)
                );
            }
            if (MockServerLogger.isEnabled(INFO) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(INFO)
                        .setMessageFormat("created expectation file watcher for{}")
                        .setArguments(initializationJsonPaths)
                );
            }
        } else {
            this.expectationSerializer = null;
            this.mockServerLogger = null;
            this.requestMatchers = null;
            this.expectationInitializerLoader = null;
        }
    }

    private synchronized void addExpectationsFromInitializer() {
        expectationInitializerLoader.retrieveExpectationsFromFile("", "exception while loading JSON initialization file with file watcher, ignoring file:{}", "updating expectations:{}from file:{}", Cause.Type.FILE_INITIALISER);
    }

    public void stop() {
        if (fileWatchers != null) {
            fileWatchers.forEach(fileWatcher -> fileWatcher.setRunning(false));
        }
    }
}
