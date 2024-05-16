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
package software.xdev.mockserver.logging;

import software.xdev.mockserver.configuration.ConfigurationProperties;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.mock.HttpState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.logging.LogManager;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.character.Character.NEW_LINE;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.*;
import static org.slf4j.event.Level.ERROR;

@Deprecated
public class MockServerLogger {

    public static void configureLogger() {
        try {
            if (System.getProperty("java.util.logging.config.file") == null && System.getProperty("java.util.logging.config.class") == null) {
                LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(("" +
                    "handlers=software.xdev.mockserver.logging.StandardOutConsoleHandler" + NEW_LINE +
                    "software.xdev.mockserver.logging.StandardOutConsoleHandler.level=ALL" + NEW_LINE +
                    "software.xdev.mockserver.logging.StandardOutConsoleHandler.formatter=java.util.logging.SimpleFormatter" + NEW_LINE +
                    "java.util.logging.SimpleFormatter.format=%1$tF %1$tT %4$s %5$s %6$s%n" + NEW_LINE +
                    "software.xdev.mockserver.level=INFO" + NEW_LINE +
                    "io.netty.level=WARNING").getBytes(UTF_8)));
                if (isNotBlank(ConfigurationProperties.javaLoggerLogLevel())) {
                    String loggingConfiguration = "" +
                        (!ConfigurationProperties.disableSystemOut() ? "handlers=software.xdev.mockserver.logging.StandardOutConsoleHandler" + NEW_LINE +
                            "software.xdev.mockserver.logging.StandardOutConsoleHandler.level=ALL" + NEW_LINE +
                            "software.xdev.mockserver.logging.StandardOutConsoleHandler.formatter=java.util.logging.SimpleFormatter" + NEW_LINE : "") +
                        "java.util.logging.SimpleFormatter.format=%1$tF %1$tT %4$s %5$s %6$s%n" + NEW_LINE +
                        "software.xdev.mockserver.level=" + ConfigurationProperties.javaLoggerLogLevel() + NEW_LINE +
                        "io.netty.level=" + (Arrays.asList("TRACE", "FINEST").contains(ConfigurationProperties.javaLoggerLogLevel()) ? "FINE" : "WARNING");
                    LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(loggingConfiguration.getBytes(UTF_8)));
                }
            }
        } catch (Throwable throwable) {
            new MockServerLogger().logEvent(
                new LogEntry()
                    .setType(SERVER_CONFIGURATION)
                    .setLogLevel(ERROR)
                    .setMessageFormat("exception while configuring Java logging - " + throwable.getMessage())
                    .setThrowable(throwable)
            );
        }
    }

    private final Logger logger;
    private HttpState httpStateHandler;

    public MockServerLogger() {
        this(MockServerLogger.class);
    }

    public MockServerLogger(final Logger logger) {
        this.logger = logger;
        this.httpStateHandler = null;
    }

    public MockServerLogger(final Class<?> loggerClass) {
        this.logger = LoggerFactory.getLogger(loggerClass);
        this.httpStateHandler = null;
    }

    public MockServerLogger(final HttpState httpStateHandler) {
        this.logger = null;
        this.httpStateHandler = httpStateHandler;
    }

    public MockServerLogger setHttpStateHandler(HttpState httpStateHandler) {
        this.httpStateHandler = httpStateHandler;
        return this;
    }

    public void logEvent(LogEntry logEntry) {
        if (logEntry.getType() == RECEIVED_REQUEST
            || logEntry.getType() == FORWARDED_REQUEST
            || logEntry.getType() == EXPECTATION_RESPONSE
            || logEntry.isAlwaysLog()
            || isEnabled(logEntry.getLogLevel())) {
            if (httpStateHandler != null) {
                httpStateHandler.log(logEntry);
            } else {
                writeToSystemOut(logger, logEntry);
            }
        }
    }

    public static void writeToSystemOut(Logger logger, LogEntry logEntry) {
        if (!ConfigurationProperties.disableLogging()) {
            if ((logEntry.isAlwaysLog() || isEnabled(logEntry.getLogLevel())) &&
                isNotBlank(logEntry.getMessage())) {
                switch (logEntry.getLogLevel()) {
                    case ERROR:
                        logger.error(portInformation(logEntry) + logEntry.getMessage(), logEntry.getThrowable());
                        break;
                    case WARN:
                        logger.warn(portInformation(logEntry) + logEntry.getMessage(), logEntry.getThrowable());
                        break;
                    case INFO:
                        logger.info(portInformation(logEntry) + logEntry.getMessage(), logEntry.getThrowable());
                        break;
                    case DEBUG:
                        logger.debug(portInformation(logEntry) + logEntry.getMessage(), logEntry.getThrowable());
                        break;
                    case TRACE:
                        logger.trace(portInformation(logEntry) + logEntry.getMessage(), logEntry.getThrowable());
                        break;
                }
            }
        }
    }

    private static String portInformation(LogEntry logEntry) {
        Integer port = logEntry.getPort();
        if (port != null) {
            return port + " ";
        } else {
            return "";
        }
    }

    @Deprecated
    public static boolean isEnabled(final Level level) {
        return isEnabled(level, ConfigurationProperties.logLevel());
    }

    @Deprecated
    public static boolean isEnabled(final Level level, final Level configuredLevel) {
        return configuredLevel != null && level.toInt() >= configuredLevel.toInt();
    }
}
