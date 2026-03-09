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
package software.xdev.mockserver.closurecallback.websocketclient;

import static software.xdev.mockserver.closurecallback.websocketregistry.WebSocketClientRegistry.WEB_SOCKET_CORRELATION_ID_HEADER_NAME;

import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import software.xdev.mockserver.logging.LoggingHandler;
import software.xdev.mockserver.mock.action.ExpectationCallback;
import software.xdev.mockserver.mock.action.ExpectationForwardAndResponseCallback;
import software.xdev.mockserver.model.HttpMessage;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpRequestAndHttpResponse;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.serialization.WebSocketMessageSerializer;
import software.xdev.mockserver.serialization.model.WebSocketClientIdDTO;
import software.xdev.mockserver.serialization.model.WebSocketErrorDTO;


@SuppressWarnings("rawtypes")
public class WebSocketClient<T extends HttpMessage>
{
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketClient.class);
	static final AttributeKey<CompletableFuture<String>> REGISTRATION_FUTURE =
		AttributeKey.valueOf("REGISTRATION_FUTURE");
	private Channel channel;
	private final WebSocketMessageSerializer webSocketMessageSerializer;
	private ExpectationCallback<T> expectationCallback;
	private ExpectationForwardAndResponseCallback expectationForwardResponseCallback;
	private boolean isStopped;
	private final EventLoopGroup eventLoopGroup;
	private final String clientId;
	public static final String CLIENT_REGISTRATION_ID_HEADER = "X-CLIENT-REGISTRATION-ID";
	
	public WebSocketClient(final EventLoopGroup eventLoopGroup, final String clientId)
	{
		this.eventLoopGroup = eventLoopGroup;
		this.clientId = clientId;
		this.webSocketMessageSerializer = new WebSocketMessageSerializer();
	}
	
	private Future<String> register(
		final InetSocketAddress serverAddress,
		final String contextPath,
		final int reconnectAttempts)
	{
		final CompletableFuture<String> registrationFuture = new CompletableFuture<>();
		try
		{
			new Bootstrap()
				.group(this.eventLoopGroup)
				.channel(NioSocketChannel.class)
				.attr(REGISTRATION_FUTURE, registrationFuture)
				.handler(new ChannelInitializer<SocketChannel>()
				{
					@Override
					protected void initChannel(final SocketChannel ch) throws URISyntaxException
					{
						ch.pipeline().addLast(new HttpClientCodec());
						ch.pipeline().addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
						ch.pipeline()
							.addLast(new WebSocketClientHandler(
								WebSocketClient.this.clientId,
								serverAddress,
								contextPath,
								WebSocketClient.this));
						// add logging
						if(LOG.isTraceEnabled())
						{
							ch.pipeline().addLast(new LoggingHandler(WebSocketClient.class.getName() + "-last"));
						}
					}
				})
				.connect(serverAddress)
				.addListener((ChannelFutureListener)connectChannelFuture -> {
					this.channel = connectChannelFuture.channel();
					this.channel.closeFuture().addListener((ChannelFutureListener)closeChannelFuture -> {
						if(!this.isStopped && reconnectAttempts > 0)
						{
							// attempt to re-connect
							this.register(serverAddress, contextPath, reconnectAttempts - 1);
						}
					});
				});
			
			// handle HttpResponseStatus.RESET_CONTENT
			
		}
		catch(final Exception e)
		{
			registrationFuture.completeExceptionally(new WebSocketException(
				"Exception while starting web socket client",
				e));
		}
		return registrationFuture;
	}
	
	@SuppressWarnings("PMD.CognitiveComplexity")
	void receivedTextWebSocketFrame(final TextWebSocketFrame textWebSocketFrame)
	{
		try
		{
			final Object deserializedMessage = this.webSocketMessageSerializer.deserialize(textWebSocketFrame.text());
			if(deserializedMessage instanceof final HttpRequest request)
			{
				final String webSocketCorrelationId = request.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
				if(LOG.isTraceEnabled())
				{
					LOG.trace(
						"Received request {} over websocket for client {} for correlationId {}",
						request,
						this.clientId,
						webSocketCorrelationId);
				}
				if(this.expectationCallback != null)
				{
					try
					{
						final T result = this.expectationCallback.handle(request);
						if(LOG.isTraceEnabled())
						{
							LOG.trace(
								"Returning response {} for request {} "
									+ "over websocket for client {} for correlationId {}",
								result,
								request,
								this.clientId,
								webSocketCorrelationId);
						}
						result.withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId);
						this.channel.writeAndFlush(new TextWebSocketFrame(this.webSocketMessageSerializer.serialize(
							result)));
					}
					catch(final Exception ex)
					{
						LOG.error("Exception thrown while handling callback for request", ex);
						this.channel.writeAndFlush(new TextWebSocketFrame(this.webSocketMessageSerializer.serialize(
							new WebSocketErrorDTO()
								.setMessage(ex.getMessage())
								.setWebSocketCorrelationId(webSocketCorrelationId)
						)));
					}
				}
			}
			else if(deserializedMessage instanceof final HttpRequestAndHttpResponse httpRequestAndHttpResponse)
			{
				final HttpRequest httpRequest = httpRequestAndHttpResponse.getHttpRequest();
				final HttpResponse httpResponse = httpRequestAndHttpResponse.getHttpResponse();
				final String webSocketCorrelationId =
					httpRequest.getFirstHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME);
				if(LOG.isTraceEnabled())
				{
					LOG.trace(
						"Received request and response {} over websocket for client {} for correlationId {}",
						httpResponse,
						this.clientId,
						webSocketCorrelationId);
				}
				if(this.expectationForwardResponseCallback != null)
				{
					try
					{
						final HttpResponse response =
							this.expectationForwardResponseCallback.handle(httpRequest, httpResponse);
						if(LOG.isTraceEnabled())
						{
							LOG.trace(
								"Returning response {} for request and response {} "
									+ "over websocket for client {} for correlationId {}",
								response,
								httpRequestAndHttpResponse,
								this.clientId,
								webSocketCorrelationId);
						}
						response.withHeader(WEB_SOCKET_CORRELATION_ID_HEADER_NAME, webSocketCorrelationId);
						this.channel.writeAndFlush(new TextWebSocketFrame(this.webSocketMessageSerializer.serialize(
							response)));
					}
					catch(final Exception ex)
					{
						LOG.error("Exception thrown while handling callback for request and response", ex);
						this.channel.writeAndFlush(new TextWebSocketFrame(this.webSocketMessageSerializer.serialize(
							new WebSocketErrorDTO()
								.setMessage(ex.getMessage())
								.setWebSocketCorrelationId(webSocketCorrelationId)
						)));
					}
				}
			}
			else if(deserializedMessage instanceof WebSocketClientIdDTO)
			{
				if(LOG.isTraceEnabled())
				{
					LOG.trace("Received client id {}", deserializedMessage);
				}
			}
			else
			{
				if(LOG.isWarnEnabled())
				{
					LOG.trace(
						"Web socket client received a message that isn't "
							+ "HttpRequest or HttpRequestAndHttpResponse {} which has been deserialized as {}",
						textWebSocketFrame.text(),
						deserializedMessage);
				}
				throw new WebSocketException("Unsupported web socket message " + textWebSocketFrame.text());
			}
		}
		catch(final Exception e)
		{
			throw new WebSocketException("Exception while receiving web socket message", e);
		}
	}
	
	public void stopClient()
	{
		this.isStopped = true;
		try
		{
			if(this.eventLoopGroup != null && !this.eventLoopGroup.isShuttingDown())
			{
				this.eventLoopGroup.shutdownGracefully();
			}
			if(this.channel != null && this.channel.isOpen())
			{
				this.channel.close().sync();
				this.channel = null;
			}
		}
		catch(final InterruptedException e)
		{
			throw new WebSocketException("Exception while closing client", e);
		}
	}
	
	public Future<String> registerExpectationCallback(
		final ExpectationCallback<T> expectationCallback,
		final ExpectationForwardAndResponseCallback expectationForwardResponseCallback,
		final InetSocketAddress serverAddress,
		final String contextPath)
	{
		if(this.expectationCallback == null)
		{
			this.expectationCallback = expectationCallback;
			this.expectationForwardResponseCallback = expectationForwardResponseCallback;
			return this.register(serverAddress, contextPath, 3);
		}
		else
		{
			throw new IllegalArgumentException(
				"It is not possible to set response callback once a forward callback has been set");
		}
	}
}
