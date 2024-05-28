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
package software.xdev.mockserver.netty;

import static io.netty.handler.codec.http.HttpHeaderNames.PROXY_AUTHENTICATE;
import static io.netty.handler.codec.http.HttpHeaderNames.PROXY_AUTHORIZATION;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED;
import static software.xdev.mockserver.exception.ExceptionHandling.closeOnFlush;
import static software.xdev.mockserver.exception.ExceptionHandling.connectionClosedException;
import static software.xdev.mockserver.mock.HttpState.PATH_PREFIX;
import static software.xdev.mockserver.model.HttpResponse.response;
import static software.xdev.mockserver.model.PortBinding.portBinding;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.enableSslUpstreamAndDownstream;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;
import static software.xdev.mockserver.util.StringUtils.isNotBlank;

import java.net.BindException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import software.xdev.mockserver.configuration.ServerConfiguration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.mock.HttpState;
import software.xdev.mockserver.mock.action.http.HttpActionHandler;
import software.xdev.mockserver.model.HttpRequest;
import software.xdev.mockserver.model.HttpResponse;
import software.xdev.mockserver.model.MediaType;
import software.xdev.mockserver.model.PortBinding;
import software.xdev.mockserver.netty.proxy.connect.HttpConnectHandler;
import software.xdev.mockserver.netty.responsewriter.NettyResponseWriter;
import software.xdev.mockserver.responsewriter.ResponseWriter;
import software.xdev.mockserver.scheduler.SchedulerThreadFactory;
import software.xdev.mockserver.serialization.Base64Converter;
import software.xdev.mockserver.serialization.PortBindingSerializer;


@ChannelHandler.Sharable
public class HttpRequestHandler extends SimpleChannelInboundHandler<HttpRequest>
{
	private static final Logger LOG = LoggerFactory.getLogger(HttpRequestHandler.class);
	
	public static final AttributeKey<Boolean> PROXYING = AttributeKey.valueOf("PROXYING");
	public static final AttributeKey<Set<String>> LOCAL_HOST_HEADERS = AttributeKey.valueOf("LOCAL_HOST_HEADERS");
	private final ServerConfiguration configuration;
	private final LifeCycle server;
	private final HttpState httpState;
	private final PortBindingSerializer portBindingSerializer;
	private final HttpActionHandler httpActionHandler;
	
	public HttpRequestHandler(
		final ServerConfiguration configuration,
		final LifeCycle server,
		final HttpState httpState,
		final HttpActionHandler httpActionHandler)
	{
		super(false);
		this.configuration = configuration;
		this.server = server;
		this.httpState = httpState;
		this.portBindingSerializer = new PortBindingSerializer();
		this.httpActionHandler = httpActionHandler;
	}
	
	private static boolean isProxyingRequest(final ChannelHandlerContext ctx)
	{
		if(ctx != null && ctx.channel() != null && ctx.channel().attr(PROXYING).get() != null)
		{
			return ctx.channel().attr(PROXYING).get();
		}
		return false;
	}
	
	public static void setProxyingRequest(final ChannelHandlerContext ctx, final Boolean value)
	{
		if(ctx != null && ctx.channel() != null)
		{
			ctx.channel().attr(PROXYING).set(value);
		}
	}
	
	private static Set<String> getLocalAddresses(final ChannelHandlerContext ctx)
	{
		if(ctx != null
			&& ctx.channel().attr(LOCAL_HOST_HEADERS) != null
			&& ctx.channel().attr(LOCAL_HOST_HEADERS).get() != null)
		{
			return ctx.channel().attr(LOCAL_HOST_HEADERS).get();
		}
		return new HashSet<>();
	}
	
	@SuppressWarnings("checkstyle:MagicNumber")
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final HttpRequest request)
	{
		final ResponseWriter responseWriter =
			new NettyResponseWriter(this.configuration, ctx, this.httpState.getScheduler());
		try
		{
			if(!this.httpState.handle(request, responseWriter, false))
			{
				
				if(request.matches("PUT", PATH_PREFIX + "/status", "/status")
					|| isNotBlank(this.configuration.livenessHttpGetPath()) && request.matches(
						"GET",
						this.configuration.livenessHttpGetPath()))
				{
					
					responseWriter.writeResponse(
						request,
						OK,
						this.portBindingSerializer.serialize(portBinding(this.server.getLocalPorts())),
						"application/json");
				}
				else if(request.matches("PUT", PATH_PREFIX + "/bind", "/bind"))
				{
					
					final PortBinding requestedPortBindings =
						this.portBindingSerializer.deserialize(request.getBodyAsString());
					if(requestedPortBindings != null)
					{
						try
						{
							final List<Integer> actualPortBindings =
								this.server.bindServerPorts(requestedPortBindings.getPorts());
							responseWriter.writeResponse(
								request,
								OK,
								this.portBindingSerializer.serialize(portBinding(actualPortBindings)),
								"application/json");
						}
						catch(final RuntimeException e)
						{
							if(e.getCause() instanceof BindException)
							{
								responseWriter.writeResponse(
									request,
									BAD_REQUEST,
									e.getMessage() + " port already in use",
									MediaType.create("text", "plain").toString());
							}
							else
							{
								throw e;
							}
						}
					}
				}
				else if(request.matches("PUT", PATH_PREFIX + "/stop", "/stop"))
				{
					ctx.writeAndFlush(response().withStatusCode(OK.code()));
					new SchedulerThreadFactory("MockServer Stop").newThread(this.server::stop).start();
				}
				else if(request.getMethod().getValue().equals("CONNECT"))
				{
					final String username = this.configuration.proxyAuthenticationUsername();
					final String password = this.configuration.proxyAuthenticationPassword();
					if(isNotBlank(username)
						&& isNotBlank(password)
						&& !request.containsHeader(
						PROXY_AUTHORIZATION.toString(),
						"Basic " + Base64Converter.bytesToBase64String(
							(username + ':' + password).getBytes(StandardCharsets.UTF_8),
							StandardCharsets.US_ASCII)))
					{
						final HttpResponse response = response()
							.withStatusCode(PROXY_AUTHENTICATION_REQUIRED.code())
							.withHeader(
								PROXY_AUTHENTICATE.toString(),
								"Basic realm=\""
									+ StringEscapeUtils.escapeJava(this.configuration.proxyAuthenticationRealm())
									+ "\", charset=\"UTF-8\"");
						ctx.writeAndFlush(response);
						LOG.info("Proxy authentication failed so returning response: {} for forwarded request: {}",
							response, request);
					}
					else
					{
						setProxyingRequest(ctx, Boolean.TRUE);
						// assume SSL for CONNECT request
						enableSslUpstreamAndDownstream(ctx.channel());
						final String[] hostParts = request.getPath().getValue().split(":");
						final int port = hostParts.length > 1
							? Integer.parseInt(hostParts[1])
							: isSslEnabledUpstream(ctx.channel()) ? 443 : 80;
						ctx.pipeline().addLast(new HttpConnectHandler(this.configuration,
							this.server, hostParts[0], port));
						ctx.pipeline().remove(this);
						ctx.fireChannelRead(request);
					}
				}
				else
				{
					try
					{
						this.httpActionHandler.processAction(
							request,
							responseWriter,
							ctx,
							getLocalAddresses(ctx),
							isProxyingRequest(ctx),
							false);
					}
					catch(final Exception ex)
					{
						LOG.error("Exception processing request: {}", request, ex);
					}
				}
			}
		}
		catch(final IllegalArgumentException iae)
		{
			LOG.error("Exception processing request: {}", request, iae);
			// send request without API CORS headers
			responseWriter.writeResponse(
				request,
				BAD_REQUEST,
				iae.getMessage(),
				MediaType.create("text", "plain").toString());
		}
		catch(final Exception ex)
		{
			LOG.error("Exception processing {}", request, ex);
			responseWriter.writeResponse(
				request,
				response().withStatusCode(BAD_REQUEST.code()).withBody(ex.getMessage()),
				true);
		}
	}
	
	@Override
	public void channelReadComplete(final ChannelHandlerContext ctx)
	{
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause)
	{
		if(connectionClosedException(cause))
		{
			LOG.error("Exception caught by {} handler -> closing pipeline {}", this.server.getClass(), ctx.channel());
		}
		closeOnFlush(ctx.channel());
	}
}
