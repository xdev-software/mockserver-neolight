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
package software.xdev.mockserver.responsewriter;

import io.netty.handler.codec.http.HttpResponseStatus;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.cors.CORSHeaders;
import software.xdev.mockserver.model.ConnectionOptions;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.mock.HttpState.PATH_PREFIX;
import static software.xdev.mockserver.model.Header.header;
import static software.xdev.mockserver.model.HttpResponse.notFoundResponse;
import static software.xdev.mockserver.model.HttpResponse.response;

public abstract class ResponseWriter {
    
    private static final Logger LOG = LoggerFactory.getLogger(ResponseWriter.class);
    
    protected final Configuration configuration;
    private final CORSHeaders corsHeaders;

    protected ResponseWriter(Configuration configuration) {
        this.configuration = configuration;
        corsHeaders = new CORSHeaders(configuration);
    }

    public void writeResponse(final HttpRequest request, final HttpResponseStatus responseStatus) {
        writeResponse(request, responseStatus, "", "application/json");
    }

    public void writeResponse(final HttpRequest request, final HttpResponseStatus responseStatus, final String body, final String contentType) {
        HttpResponse response = response()
            .withStatusCode(responseStatus.code())
            .withReasonPhrase(responseStatus.reasonPhrase())
            .withBody(body);
        if (body != null && !body.isEmpty()) {
            response.replaceHeader(header(CONTENT_TYPE.toString(), contentType + "; charset=utf-8"));
        }
        writeResponse(request, response, true);
    }

    public void writeResponse(final HttpRequest request, HttpResponse response, final boolean apiResponse) {
        if (response == null) {
            response = notFoundResponse();
        }
        if (configuration.enableCORSForAllResponses()) {
            corsHeaders.addCORSHeaders(request, response);
        } else if (apiResponse && configuration.enableCORSForAPI()) {
            corsHeaders.addCORSHeaders(request, response);
        }
        String contentLengthHeader = response.getFirstHeader(CONTENT_LENGTH.toString());
        if (isNotBlank(contentLengthHeader)) {
            try {
                int contentLength = Integer.parseInt(contentLengthHeader);
                if (response.getBodyAsRawBytes().length > contentLength) {
                    LOG.info("Returning response with content-length header {} "
                        + "which is smaller then response body length {}, "
                        + "body will likely be truncated by client receiving request",
                        contentLength,
                        response.getBodyAsRawBytes().length);
                }
            } catch (NumberFormatException ignore) {
                // ignore exception while parsing invalid content-length header
            }
        }
        if (apiResponse) {
            final String path = request.getPath().getValue();
            if (!path.startsWith(PATH_PREFIX) && !path.equals(configuration.livenessHttpGetPath())) {
                response.withHeader("deprecated",
                    "\"" + path + "\" is deprecated use \"" + PATH_PREFIX + path + "\" instead");
            }
        }

        // send response down the request HTTP2 stream
        if (request.getStreamId() != null) {
            response.withStreamId(request.getStreamId());
        }

        sendResponse(request, addConnectionHeader(request, response));
    }

    public abstract void sendResponse(HttpRequest request, HttpResponse response);

    protected HttpResponse addConnectionHeader(final HttpRequest request, final HttpResponse response) {
        ConnectionOptions connectionOptions = response.getConnectionOptions();

        HttpResponse responseWithConnectionHeader = response.clone();

        if (connectionOptions != null && (connectionOptions.getSuppressConnectionHeader() != null || connectionOptions.getKeepAliveOverride() != null)) {
            if (!Boolean.TRUE.equals(connectionOptions.getSuppressConnectionHeader())) {
                if (Boolean.TRUE.equals(connectionOptions.getKeepAliveOverride())) {
                    responseWithConnectionHeader.replaceHeader(header(CONNECTION.toString(), KEEP_ALIVE.toString()));
                } else {
                    responseWithConnectionHeader.replaceHeader(header(CONNECTION.toString(), CLOSE.toString()));
                }
            }
        } else {
            if (Boolean.TRUE.equals(request.isKeepAlive())) {
                responseWithConnectionHeader.replaceHeader(header(CONNECTION.toString(), KEEP_ALIVE.toString()));
            } else {
                responseWithConnectionHeader.replaceHeader(header(CONNECTION.toString(), CLOSE.toString()));
            }
        }

        return responseWithConnectionHeader;
    }
}
