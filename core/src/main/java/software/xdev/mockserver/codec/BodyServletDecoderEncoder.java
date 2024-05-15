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
package software.xdev.mockserver.codec;

import com.google.common.io.ByteStreams;
import software.xdev.mockserver.log.model.LogEntry;
import software.xdev.mockserver.logging.MockServerLogger;
import software.xdev.mockserver.model.Body;
import software.xdev.mockserver.model.BodyWithContentType;
import software.xdev.mockserver.streams.IOStreamUtils;
import org.slf4j.event.Level;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

@SuppressWarnings("rawtypes")
public class BodyServletDecoderEncoder {

    private final MockServerLogger mockServerLogger;
    private final IOStreamUtils ioStreamUtils;
    private final BodyDecoderEncoder bodyDecoderEncoder = new BodyDecoderEncoder();

    public BodyServletDecoderEncoder(MockServerLogger mockServerLogger) {
        this.mockServerLogger = mockServerLogger;
        this.ioStreamUtils = new IOStreamUtils(mockServerLogger);
    }

    public void bodyToServletResponse(HttpServletResponse httpServletResponse, Body body, String contentTypeHeader) {
        byte[] bytes = bodyDecoderEncoder.bodyToBytes(body, contentTypeHeader);
        if (bytes != null) {
            ioStreamUtils.writeToOutputStream(bytes, httpServletResponse);
        }
    }

    public BodyWithContentType servletRequestToBody(HttpServletRequest servletRequest) {
        if (servletRequest != null) {
            String contentTypeHeader = servletRequest.getHeader(CONTENT_TYPE.toString());
            try {
                byte[] bodyBytes = ByteStreams.toByteArray(servletRequest.getInputStream());
                return bodyDecoderEncoder.bytesToBody(bodyBytes, contentTypeHeader);
            } catch (Throwable throwable) {
                mockServerLogger.logEvent(
                    new LogEntry()
                        .setLogLevel(Level.ERROR)
                        .setMessageFormat("exception while reading HttpServletRequest input stream")
                        .setThrowable(throwable)
                );
                throw new RuntimeException("IOException while reading HttpServletRequest input stream", throwable);
            }
        }
        return null;
    }
}
