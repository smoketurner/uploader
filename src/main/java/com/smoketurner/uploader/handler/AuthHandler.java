/**
 * Copyright 2016 Smoke Turner, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.smoketurner.uploader.handler;

import java.security.Principal;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;

@Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AuthHandler.class);

    private final boolean clientAuth;

    /**
     * Constructor
     *
     * @param clientAuth
     *            Client authentication enabled
     */
    public AuthHandler(final boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        if (clientAuth && evt == SslHandshakeCompletionEvent.SUCCESS) {
            final SSLSession session = ctx.pipeline().get(SslHandler.class)
                    .engine().getSession();
            final Principal principal = session.getPeerPrincipal();
            LOGGER.info("Peer Principal: {}", principal);
        } else if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
            LOGGER.debug("No data received on channel, closing");
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("New connection from: <{}>",
                ctx.channel().remoteAddress().toString());
        ctx.fireChannelActive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof NotSslRecordException) {
            LOGGER.error("Invalid SSL/TLS record, closing channel");
        } else {
            LOGGER.error("Exception occurred, closing channel", cause);
        }
        ctx.close();
    }
}
