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
package software.xdev.mockserver.file;

import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;

import java.io.File;
import java.io.IOException;

import static org.slf4j.event.Level.ERROR;

public class FileCreator {

    private static final MockServerLogger MOCK_SERVER_LOGGER = new MockServerLogger(FileCreator.class);

    public static File createFileIfNotExists(String type, File file) {
        if (!file.exists()) {
            try {
                createParentDirs(file);
                if (!file.createNewFile()) {
                    MOCK_SERVER_LOGGER.logEvent(
                        new LogEntry()
                            .setLogLevel(ERROR)
                            .setMessageFormat("failed to create the file{}while attempting to save " + type + " file")
                            .setArguments(file.getAbsolutePath())
                    );

                }
            } catch (Throwable throwable) {
                MOCK_SERVER_LOGGER.logEvent(
                    new LogEntry()
                        .setLogLevel(ERROR)
                        .setMessageFormat("failed to create the file{}while attempting to save " + type + " file")
                        .setArguments(file.getAbsolutePath())
                        .setThrowable(throwable)
                );
            }
        }
        return file;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void createParentDirs(File file) throws IOException {
        File parent = file.getCanonicalFile().getParentFile();
        if (parent == null) {
            /*
             * The given directory is a filesystem root. All zero of its ancestors exist. This doesn't
             * mean that the root itself exists -- consider x:\ on a Windows machine without such a drive
             * -- or even that the caller can create it, but this method makes no such guarantees even for
             * non-root files.
             */
            return;
        }
        createParentDirs(parent);
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (!parent.isDirectory()) {
            throw new IOException("Unable to create parent directories of " + file);
        }
    }

}
