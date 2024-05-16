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
package software.xdev.mockserver.netty.proxy.socks;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.ssl.SslHandler;
import software.xdev.mockserver.configuration.Configuration;
import software.xdev.mockserver.lifecycle.LifeCycle;
import software.xdev.mockserver.log.model.LogEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static software.xdev.mockserver.util.StringUtils.isNotBlank;
import static software.xdev.mockserver.log.model.LogEntry.LogMessageType.AUTHENTICATION_FAILED;
import static software.xdev.mockserver.netty.unification.PortUnificationHandler.isSslEnabledUpstream;

@ChannelHandler.Sharable
public class Socks5ProxyHandler extends SocksProxyHandler<Socks5Message> {
    
    private static final Logger LOG = LoggerFactory.getLogger(Socks5ProxyHandler.class);
    
    public Socks5ProxyHandler(Configuration configuration, LifeCycle server) {
        super(configuration, server);
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, Socks5Message socksRequest) {
        if (socksRequest instanceof Socks5InitialRequest) {
            handleInitialRequest(ctx, (Socks5InitialRequest) socksRequest);
        } else if (socksRequest instanceof Socks5PasswordAuthRequest) {
            handlePasswordAuthRequest(ctx, (Socks5PasswordAuthRequest) socksRequest);
        } else if (socksRequest instanceof Socks5CommandRequest) {
            handleCommandRequest(ctx, (Socks5CommandRequest) socksRequest);
        } else {
            ctx.close();
        }
    }

    private void handleInitialRequest(ChannelHandlerContext ctx, Socks5InitialRequest initialRequest) {
        String username = configuration.proxyAuthenticationUsername();
        String password = configuration.proxyAuthenticationPassword();
        Socks5AuthMethod requiredAuthMethod;
        ChannelHandler nextRequestDecoder;
        if (initialRequest.authMethods().contains(Socks5AuthMethod.NO_AUTH)) {
            requiredAuthMethod = Socks5AuthMethod.NO_AUTH;
            nextRequestDecoder = new Socks5CommandRequestDecoder();
        } else if (initialRequest.authMethods().contains(Socks5AuthMethod.PASSWORD)) {
            if (isNotBlank(username) && isNotBlank(password)) {
                requiredAuthMethod = Socks5AuthMethod.PASSWORD;
                nextRequestDecoder = new Socks5PasswordAuthRequestDecoder();
            } else {
                requiredAuthMethod = Socks5AuthMethod.NO_AUTH;
                nextRequestDecoder = new Socks5CommandRequestDecoder();
            }
        } else {
            requiredAuthMethod = Socks5AuthMethod.NO_AUTH;
            nextRequestDecoder = new Socks5CommandRequestDecoder();
        }
        answerInitialRequest(ctx, initialRequest, requiredAuthMethod, nextRequestDecoder);
    }

    private void answerInitialRequest(ChannelHandlerContext ctx, Socks5InitialRequest initialRequest, Socks5AuthMethod requiredAuthMethod, ChannelHandler nextRequestDecoder) {
        ctx.writeAndFlush(initialRequest
            .authMethods()
            .stream()
            .filter(authMethod -> authMethod.equals(requiredAuthMethod))
            .findFirst()
            .map(authMethod -> {
                if (isSslEnabledUpstream(ctx.channel())) {
                    ctx.pipeline().addAfter("SslHandler#0", null, nextRequestDecoder);
                } else {
                    ctx.pipeline().addFirst(nextRequestDecoder);
                }
                return new DefaultSocks5InitialResponse(requiredAuthMethod);
            })
            .orElse(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED))
        );
        ctx.pipeline().remove(Socks5InitialRequestDecoder.class);
    }

    private void handlePasswordAuthRequest(ChannelHandlerContext ctx, Socks5PasswordAuthRequest passwordAuthRequest) {
        String username = configuration.proxyAuthenticationUsername();
        String password = configuration.proxyAuthenticationPassword();
        // we need the null-check again here, in case the properties got unset between init and auth request
        if (isNotBlank(username) && isNotBlank(password)
            && username.equals(passwordAuthRequest.username()) && password.equals(passwordAuthRequest.password())) {
            ctx.pipeline().replace(Socks5PasswordAuthRequestDecoder.class, null, new Socks5CommandRequestDecoder());
            ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS)).awaitUninterruptibly();
        } else {
            ctx.writeAndFlush(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE)).addListener(ChannelFutureListener.CLOSE);
            LOG.info("Proxy authentication failed so returning SOCKS FAILURE response");
        }
    }

    private void handleCommandRequest(ChannelHandlerContext ctx, final Socks5CommandRequest commandRequest) {
        if (commandRequest.type().equals(Socks5CommandType.CONNECT)) { // IN HERE
            forwardConnection(ctx, new Socks5ConnectHandler(configuration, server, commandRequest.dstAddr(), commandRequest.dstPort()));
            ctx.fireChannelRead(commandRequest);
        } else {
            ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, Socks5AddressType.DOMAIN, "", 0)).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
