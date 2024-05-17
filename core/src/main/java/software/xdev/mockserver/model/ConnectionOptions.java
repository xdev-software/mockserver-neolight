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
package software.xdev.mockserver.model;

import java.util.Objects;


public class ConnectionOptions extends ObjectWithJsonToString
{
	private Boolean suppressContentLengthHeader;
	private Integer contentLengthHeaderOverride;
	private Boolean suppressConnectionHeader;
	private Integer chunkSize;
	private Boolean keepAliveOverride;
	private Boolean closeSocket;
	private Delay closeSocketDelay;
	
	public static ConnectionOptions connectionOptions()
	{
		return new ConnectionOptions();
	}
	
	/**
	 * Prevent a "Content-Length" header from being added to the response
	 *
	 * @param suppressContentLengthHeader if true no "Content-Length" header will be added to the response
	 */
	public ConnectionOptions withSuppressContentLengthHeader(final Boolean suppressContentLengthHeader)
	{
		this.suppressContentLengthHeader = suppressContentLengthHeader;
		return this;
	}
	
	public Boolean getSuppressContentLengthHeader()
	{
		return this.suppressContentLengthHeader;
	}
	
	/**
	 * Override the "Content-Length" header with the specified amount, if not set the "Content-Length" header will have
	 * a value determined by the length of the body
	 *
	 * @param contentLengthHeaderOverride the value to use for the "Content-Length" header
	 */
	public ConnectionOptions withContentLengthHeaderOverride(final Integer contentLengthHeaderOverride)
	{
		this.contentLengthHeaderOverride = contentLengthHeaderOverride;
		return this;
	}
	
	public Integer getContentLengthHeaderOverride()
	{
		return this.contentLengthHeaderOverride;
	}
	
	/**
	 * Prevent a "Connection" header from being added to the response
	 *
	 * @param suppressConnectionHeader if true no "Connection" header will be added to the response
	 */
	public ConnectionOptions withSuppressConnectionHeader(final Boolean suppressConnectionHeader)
	{
		this.suppressConnectionHeader = suppressConnectionHeader;
		return this;
	}
	
	public Boolean getSuppressConnectionHeader()
	{
		return this.suppressConnectionHeader;
	}
	
	/**
	 * Specifies the size of chunks in a response:
	 * <p>
	 * If null (the default value), zero or negative then response will not be chunked If positive and non-null the
	 * response will have "Transfer-Encoding: chunked" header and will be chunked into chunks of the size specified (as
	 * long as the body is large enough)
	 *
	 * @param chunkSize the size of response chunks, if null response will not be chunked
	 */
	public ConnectionOptions withChunkSize(final Integer chunkSize)
	{
		this.chunkSize = chunkSize;
		return this;
	}
	
	public Integer getChunkSize()
	{
		return this.chunkSize;
	}
	
	/**
	 * Override the "Connection" header: if true the "Connection" header is specified with a value of "keep-alive" if
	 * false the "Connection" header is specified with a value of "close" if not set the "Connection" header will
	 * have a
	 * a value of "close" unless the request received is HTTP 1.1 and contains a "Connection" header with a value of
	 * "keep-alive"
	 *
	 * @param keepAliveOverride if true "keep-alive" is used if false "close" is used for the "Connection" header
	 */
	public ConnectionOptions withKeepAliveOverride(final Boolean keepAliveOverride)
	{
		this.keepAliveOverride = keepAliveOverride;
		return this;
	}
	
	public Boolean getKeepAliveOverride()
	{
		return this.keepAliveOverride;
	}
	
	/**
	 * Override whether the socket is closed after a response is sent: if true the socket will always be closed, if
	 * false the socket will never be closed, if not set the socket will be closed unless the request received is HTTP
	 * 1.1 and contains a "Connection" header with a value of "keep-alive"
	 *
	 * @param closeSocket set whether the socket is closed after a response is sent
	 */
	public ConnectionOptions withCloseSocket(final Boolean closeSocket)
	{
		this.closeSocket = closeSocket;
		return this;
	}
	
	public Boolean getCloseSocket()
	{
		return this.closeSocket;
	}
	
	/**
	 * Override how long to delay before closing socket, this value is ignored if socket isn't going to be closed
	 *
	 * @param closeSocketDelay set delay before closing socket
	 */
	public ConnectionOptions withCloseSocketDelay(final Delay closeSocketDelay)
	{
		this.closeSocketDelay = closeSocketDelay;
		return this;
	}
	
	public Delay getCloseSocketDelay()
	{
		return this.closeSocketDelay;
	}
	
	@Override
	public boolean equals(final Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(!(o instanceof final ConnectionOptions that))
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
