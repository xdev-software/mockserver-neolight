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

import java.util.Objects;

import software.xdev.mockserver.model.ConnectionOptions;
import software.xdev.mockserver.model.ObjectWithJsonToString;


@SuppressWarnings("UnusedReturnValue")
public class ConnectionOptionsDTO extends ObjectWithJsonToString implements DTO<ConnectionOptions>
{
	private Boolean suppressContentLengthHeader;
	private Integer contentLengthHeaderOverride;
	private Boolean suppressConnectionHeader;
	private Integer chunkSize;
	private Boolean keepAliveOverride;
	private Boolean closeSocket;
	private DelayDTO closeSocketDelay;
	
	public ConnectionOptionsDTO(final ConnectionOptions connectionOptions)
	{
		if(connectionOptions != null)
		{
			this.suppressContentLengthHeader = connectionOptions.getSuppressContentLengthHeader();
			this.contentLengthHeaderOverride = connectionOptions.getContentLengthHeaderOverride();
			this.suppressConnectionHeader = connectionOptions.getSuppressConnectionHeader();
			this.chunkSize = connectionOptions.getChunkSize();
			this.keepAliveOverride = connectionOptions.getKeepAliveOverride();
			this.closeSocket = connectionOptions.getCloseSocket();
			if(connectionOptions.getCloseSocketDelay() != null)
			{
				this.closeSocketDelay = new DelayDTO(connectionOptions.getCloseSocketDelay());
			}
		}
	}
	
	public ConnectionOptionsDTO()
	{
	}
	
	@Override
	public ConnectionOptions buildObject()
	{
		return new ConnectionOptions()
			.withSuppressContentLengthHeader(this.suppressContentLengthHeader)
			.withContentLengthHeaderOverride(this.contentLengthHeaderOverride)
			.withSuppressConnectionHeader(this.suppressConnectionHeader)
			.withChunkSize(this.chunkSize)
			.withKeepAliveOverride(this.keepAliveOverride)
			.withCloseSocket(this.closeSocket)
			.withCloseSocketDelay(this.closeSocketDelay != null ? this.closeSocketDelay.buildObject() : null);
	}
	
	public Boolean getSuppressContentLengthHeader()
	{
		return this.suppressContentLengthHeader;
	}
	
	public ConnectionOptionsDTO setSuppressContentLengthHeader(final Boolean suppressContentLengthHeader)
	{
		this.suppressContentLengthHeader = suppressContentLengthHeader;
		return this;
	}
	
	public Integer getContentLengthHeaderOverride()
	{
		return this.contentLengthHeaderOverride;
	}
	
	public ConnectionOptionsDTO setContentLengthHeaderOverride(final Integer contentLengthHeaderOverride)
	{
		this.contentLengthHeaderOverride = contentLengthHeaderOverride;
		return this;
	}
	
	public Boolean getSuppressConnectionHeader()
	{
		return this.suppressConnectionHeader;
	}
	
	public ConnectionOptionsDTO setSuppressConnectionHeader(final Boolean suppressConnectionHeader)
	{
		this.suppressConnectionHeader = suppressConnectionHeader;
		return this;
	}
	
	public Integer getChunkSize()
	{
		return this.chunkSize;
	}
	
	public ConnectionOptionsDTO setChunkSize(final Integer chunkSize)
	{
		this.chunkSize = chunkSize;
		return this;
	}
	
	public Boolean getKeepAliveOverride()
	{
		return this.keepAliveOverride;
	}
	
	public ConnectionOptionsDTO setKeepAliveOverride(final Boolean keepAliveOverride)
	{
		this.keepAliveOverride = keepAliveOverride;
		return this;
	}
	
	public Boolean getCloseSocket()
	{
		return this.closeSocket;
	}
	
	public ConnectionOptionsDTO setCloseSocket(final Boolean closeSocket)
	{
		this.closeSocket = closeSocket;
		return this;
	}
	
	public DelayDTO getCloseSocketDelay()
	{
		return this.closeSocketDelay;
	}
	
	public ConnectionOptionsDTO setCloseSocketDelay(final DelayDTO closeSocketDelay)
	{
		this.closeSocketDelay = closeSocketDelay;
		return this;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final ConnectionOptionsDTO that))
		{
			return false;
		}
		return Objects.equals(this.getSuppressContentLengthHeader(), that.getSuppressContentLengthHeader())
			&& Objects.equals(this.getContentLengthHeaderOverride(), that.getContentLengthHeaderOverride())
			&& Objects.equals(this.getSuppressConnectionHeader(), that.getSuppressConnectionHeader())
			&& Objects.equals(this.getChunkSize(), that.getChunkSize())
			&& Objects.equals(this.getKeepAliveOverride(), that.getKeepAliveOverride())
			&& Objects.equals(this.getCloseSocket(), that.getCloseSocket())
			&& Objects.equals(this.getCloseSocketDelay(), that.getCloseSocketDelay());
	}
	
	@Override
	public int hashCode()
	{
		return Objects.hash(
			this.getSuppressContentLengthHeader(),
			this.getContentLengthHeaderOverride(),
			this.getSuppressConnectionHeader(),
			this.getChunkSize(),
			this.getKeepAliveOverride(),
			this.getCloseSocket(),
			this.getCloseSocketDelay());
	}
}
