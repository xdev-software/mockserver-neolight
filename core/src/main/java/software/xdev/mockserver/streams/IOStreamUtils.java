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
package software.xdev.mockserver.streams;

import com.google.common.io.ByteStreams;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import org.slf4j.event.Level;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static software.xdev.mockserver.character.Character.NEW_LINE;

public class IOStreamUtils {
    private final MockServerLogger mockServerLogger;

    public IOStreamUtils(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
    }

    public static String readHttpInputStreamToString(Socket socket) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            Integer contentLength = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("content-length") || line.startsWith("Content-Length")) {
                    contentLength = Integer.parseInt(line.split(":")[1].trim());
                }
                if (line.length() == 0) {
                    if (contentLength != null) {
                        result.append(NEW_LINE);
                        for (int position = 0; position < contentLength; position++) {
                            result.append((char) bufferedReader.read());
                        }
                    }
                    break;
                }
                result.append(line).append(NEW_LINE);
            }
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readSocketToString(Socket socket) {
        StringBuilder result = new StringBuilder();
        try {
            InputStream inputStream = socket.getInputStream();
            do {
                final byte[] buffer = new byte[10000];
                final int readBytes = inputStream.read(buffer);
                result.append(new String(
                    Arrays.copyOfRange(buffer, 0, readBytes),
                    StandardCharsets.UTF_8
                ));
            } while (inputStream.available() > 0);
            return result.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readHttpInputStreamToString(ServletRequest request) {
        try {
            return new String(ByteStreams.toByteArray(request.getInputStream()), request.getCharacterEncoding() != null ? request.getCharacterEncoding() : UTF_8.name());
        } catch (IOException ioe) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("IOException while reading HttpServletRequest input stream")
                    .setThrowable(ioe)
            );
            throw new RuntimeException("IOException while reading HttpServletRequest input stream", ioe);
        }
    }

    public void writeToOutputStream(byte[] data, ServletResponse response) {
        try {
            OutputStream output = response.getOutputStream();
            output.write(data);
            output.close();
        } catch (IOException ioe) {
            mockServerLogger.logEvent(
                new LogEntry()
                    .setLogLevel(Level.ERROR)
                    .setMessageFormat("IOException while writing [" + new String(data) + "] to HttpServletResponse output stream")
                    .setThrowable(ioe)
            );
            throw new RuntimeException("IOException while writing [" + new String(data) + "] to HttpServletResponse output stream", ioe);
        }
    }

}
