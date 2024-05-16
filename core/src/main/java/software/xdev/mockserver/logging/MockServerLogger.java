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
    }


    public static void writeToSystemOut(Logger logger, LogEntry logEntry) {
        // NOOP
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
        return false;
    }

    @Deprecated
    public static boolean isEnabled(final Level level, final Level configuredLevel) {
        return false;
    }
}
