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
package software.xdev.mockserver.serialization.model;

import software.xdev.mockserver.model.ConnectionOptions;
import software.xdev.mockserver.model.Delay;
import software.xdev.mockserver.model.ObjectWithJsonToString;

@SuppressWarnings("UnusedReturnValue")
public class ConnectionOptionsDTO extends ObjectWithJsonToString implements DTO<ConnectionOptions> {

    private Boolean suppressContentLengthHeader = null;
    private Integer contentLengthHeaderOverride = null;
    private Boolean suppressConnectionHeader = null;
    private Integer chunkSize = null;
    private Boolean keepAliveOverride = null;
    private Boolean closeSocket = null;
    private DelayDTO closeSocketDelay = null;

    public ConnectionOptionsDTO(ConnectionOptions connectionOptions) {
        if (connectionOptions != null) {
            suppressContentLengthHeader = connectionOptions.getSuppressContentLengthHeader();
            contentLengthHeaderOverride = connectionOptions.getContentLengthHeaderOverride();
            suppressConnectionHeader = connectionOptions.getSuppressConnectionHeader();
            chunkSize = connectionOptions.getChunkSize();
            keepAliveOverride = connectionOptions.getKeepAliveOverride();
            closeSocket = connectionOptions.getCloseSocket();
            if (connectionOptions.getCloseSocketDelay() != null) {
                closeSocketDelay = new DelayDTO(connectionOptions.getCloseSocketDelay());
            }
        }
    }

    public ConnectionOptionsDTO() {
    }

    public ConnectionOptions buildObject() {
        return new ConnectionOptions()
            .withSuppressContentLengthHeader(suppressContentLengthHeader)
            .withContentLengthHeaderOverride(contentLengthHeaderOverride)
            .withSuppressConnectionHeader(suppressConnectionHeader)
            .withChunkSize(chunkSize)
            .withKeepAliveOverride(keepAliveOverride)
            .withCloseSocket(closeSocket)
            .withCloseSocketDelay(closeSocketDelay != null ? closeSocketDelay.buildObject() : null);
    }

    public Boolean getSuppressContentLengthHeader() {
        return suppressContentLengthHeader;
    }

    public ConnectionOptionsDTO setSuppressContentLengthHeader(Boolean suppressContentLengthHeader) {
        this.suppressContentLengthHeader = suppressContentLengthHeader;
        return this;
    }

    public Integer getContentLengthHeaderOverride() {
        return contentLengthHeaderOverride;
    }

    public ConnectionOptionsDTO setContentLengthHeaderOverride(Integer contentLengthHeaderOverride) {
        this.contentLengthHeaderOverride = contentLengthHeaderOverride;
        return this;
    }

    public Boolean getSuppressConnectionHeader() {
        return suppressConnectionHeader;
    }

    public ConnectionOptionsDTO setSuppressConnectionHeader(Boolean suppressConnectionHeader) {
        this.suppressConnectionHeader = suppressConnectionHeader;
        return this;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public ConnectionOptionsDTO setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
        return this;
    }

    public Boolean getKeepAliveOverride() {
        return keepAliveOverride;
    }

    public ConnectionOptionsDTO setKeepAliveOverride(Boolean keepAliveOverride) {
        this.keepAliveOverride = keepAliveOverride;
        return this;
    }

    public Boolean getCloseSocket() {
        return closeSocket;
    }

    public ConnectionOptionsDTO setCloseSocket(Boolean closeSocket) {
        this.closeSocket = closeSocket;
        return this;
    }

    public DelayDTO getCloseSocketDelay() {
        return closeSocketDelay;
    }

    public ConnectionOptionsDTO setCloseSocketDelay(DelayDTO closeSocketDelay) {
        this.closeSocketDelay = closeSocketDelay;
        return this;
    }
}
