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

import com.fasterxml.jackson.databind.ObjectWriter;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.file.FilePath;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.mock.Expectation;
import software.xdev.mockserver.mock.RequestMatchers;
import software.xdev.mockserver.mock.listeners.MockServerMatcherListener;
import software.xdev.mockserver.mock.listeners.MockServerMatcherNotifier;
import software.xdev.mockserver.serialization.serializers.response.TimeToLiveSerializer;
import org.slf4j.event.Level;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.xdev.mockserver.serialization.ObjectMapperFactory.createObjectMapper;
import static org.slf4j.event.Level.*;

public class ExpectationFileSystemPersistence implements MockServerMatcherListener {

    private final Configuration configuration;
    private final MockServerLogger mockServerLogger;
    private final RequestMatchers requestMatchers;
    private final ObjectWriter objectWriter;
    private final Path filePath;
    private final boolean initializationPathMatchesPersistencePath;
    private final ReentrantLock fileWriteLock = new ReentrantLock();

    public ExpectationFileSystemPersistence(Configuration configuration, MockServerLogger mockServerLogger, RequestMatchers requestMatchers) {
        this.configuration = configuration;
        if (configuration.persistExpectations()) {
            this.mockServerLogger = mockServerLogger;
            this.requestMatchers = requestMatchers;
            this.objectWriter = createObjectMapper(true, false, new TimeToLiveSerializer());
            this.filePath = Paths.get(configuration.persistedExpectationsPath());
            try {
                Files.createFile(filePath);
            } catch (FileAlreadyExistsException ignore) {
            } catch (Throwable throwable) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception creating persisted expectations file " + filePath)
                        .setThrowable(throwable)
                );
            }
            this.initializationPathMatchesPersistencePath = FilePath.expandFilePathGlobs(configuration.initializationJsonPath()).contains(configuration.persistedExpectationsPath());
            requestMatchers.registerListener(this);
            if (MockServerLogger.isEnabled(INFO) && mockServerLogger != null) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(INFO)
                        .setMessageFormat("created expectation file system persistence for{}")
                        .setArguments(configuration.persistedExpectationsPath())
                );
            }
        } else {
            this.mockServerLogger = null;
            this.requestMatchers = null;
            this.objectWriter = null;
            this.filePath = null;
            this.initializationPathMatchesPersistencePath = true;
        }
    }

    @Override
    public void updated(RequestMatchers requestMatchers, MockServerMatcherNotifier.Cause cause) {
        // ignore non-API changes from the same file
        if (cause == MockServerMatcherNotifier.Cause.API || cause.getType() == MockServerMatcherNotifier.Cause.Type.CLASS_INITIALISER || !initializationPathMatchesPersistencePath) {
            fileWriteLock.lock();
            try {
                try {
                    try (
                        FileOutputStream fileOutputStream = new FileOutputStream(filePath.toFile());
                        FileChannel fileChannel = fileOutputStream.getChannel();
                        FileLock fileLock = fileChannel.lock()
                    ) {
                        if (fileLock != null) {
                            List<Expectation> expectations = requestMatchers.retrieveActiveExpectations(null);
                            if (MockServerLogger.isEnabled(TRACE) && mockServerLogger != null) {
                                mockServerLogger.logEvent(
                                    new LogEntry()
                                        .setLogLevel(TRACE)
                                        .setMessageFormat("persisting expectations{}to{}")
                                        .setArguments(expectations, configuration.persistedExpectationsPath())
                                );
                            } else if (MockServerLogger.isEnabled(DEBUG) && mockServerLogger != null) {
                                mockServerLogger.logEvent(
                                    new LogEntry()
                                        .setLogLevel(DEBUG)
                                        .setMessageFormat("persisting expectations to{}")
                                        .setArguments(configuration.persistedExpectationsPath())
                                );
                            }
                            byte[] data = serialize(expectations).getBytes(UTF_8);
                            ByteBuffer buffer = ByteBuffer.wrap(data);
                            buffer.put(data);
                            buffer.rewind();
                            while (buffer.hasRemaining()) {
                                fileChannel.write(buffer);
                            }
                        }
                    }
                } catch (Throwable throwable) {
                    mockServerLogger.logEvent(
                        new LogEntry()
                            .setLogLevel(Level.ERROR)
                            .setMessageFormat("exception while persisting expectations to " + filePath.toString())
                            .setThrowable(throwable)
                    );
                }
            } finally {
                fileWriteLock.unlock();
            }
        }
    }

    public String serialize(List<Expectation> expectations) {
        return serialize(expectations.toArray(new Expectation[0]));
    }

    public String serialize(Expectation... expectations) {
        try {
            if (expectations != null && expectations.length > 0) {
                return objectWriter.writeValueAsString(expectations);
            } else {
                return "[]";
            }
        } catch (Exception e) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("exception while serializing expectation to JSON with value " + Arrays.asList(expectations))
                    .setThrowable(e)
            );
            throw new RuntimeException("Exception while serializing expectation to JSON with value " + Arrays.asList(expectations), e);
        }
    }

    public void stop() {
        if (requestMatchers != null) {
            requestMatchers.unregisterListener(this);
        }
    }
}
