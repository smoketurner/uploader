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
import java.util.Locale;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Strings;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;

@Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AuthHandler.class);
    public static final AttributeKey<String> CUSTOMER_KEY = AttributeKey
            .valueOf("customer_id");
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

        // If we are requiring mutual authentication and the SSL handshake has
        // been completed, extract the principal from the client certificate and
        // fire the channel active event on the batch handler to prepare an
        // empty batch.
        if (clientAuth && evt == SslHandshakeCompletionEvent.SUCCESS) {
            final SSLSession session = ctx.pipeline().get(SslHandler.class)
                    .engine().getSession();
            final Principal principal = session.getPeerPrincipal();
            LOGGER.info("Peer Principal: {}", principal);

            // removes CN= from the principal
            final String customerId = Strings
                    .emptyToNull(getCustomerId(principal));

            ctx.channel().attr(CUSTOMER_KEY).set(customerId);
            ctx.fireChannelActive();
        } else if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
            LOGGER.warn("No data received on channel, closing");
            ctx.close();
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("New connection from: <{}>",
                ctx.channel().remoteAddress().toString());
        if (!clientAuth) {
            // If we aren't mutually authenticating the client certificates,
            // then immediately fire the channel active event on the batch
            // handler to prepare a new batch. Otherwise, this is fired after
            // the SSL handshake has been completed.
            ctx.fireChannelActive();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof NotSslRecordException) {
            LOGGER.warn("Invalid SSL/TLS record on channel, closing");
        } else {
            LOGGER.error("Exception occurred on channel, closing", cause);
        }
        ctx.close();
    }

    /**
     * Sanitize the customer ID out of the SSL certificate principal
     *
     * @param principal
     *            Principal
     * @return customer ID or null if not found
     */
    private static String getCustomerId(@Nullable final Principal principal) {
        if (principal == null) {
            return null;
        }

        final String name = Strings.nullToEmpty(principal.getName());
        if (name.startsWith("CN=")) {
            return name.substring(3).trim().toLowerCase(Locale.ENGLISH);
        }
        return name.trim().toLowerCase(Locale.ENGLISH);
    }
}
