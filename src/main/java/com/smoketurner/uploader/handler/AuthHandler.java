/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
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
package com.smoketurner.uploader.handler;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.ssl.NotSslRecordException;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthHandler extends ChannelInboundHandlerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthHandler.class);
  // this is public so we can access it again in the BatchHandler
  public static final AttributeKey<String> CUSTOMER_KEY = AttributeKey.valueOf("customer_id");
  private final boolean clientAuth;

  /**
   * Constructor
   *
   * @param clientAuth Client authentication enabled
   */
  public AuthHandler(final boolean clientAuth) {
    this.clientAuth = clientAuth;
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    LOGGER.trace("userEventTriggered: {}", evt.getClass().getName());

    // if the channel has been idle, close it
    if (evt == IdleStateEvent.FIRST_READER_IDLE_STATE_EVENT) {
      LOGGER.warn("No data received on channel, closing");
      ctx.close();
      return;
    } else if (evt == SslHandshakeCompletionEvent.SUCCESS) {
      // If we require mutual authentication, extract the principal from
      // the client certificate and store it in the channel attributes.
      if (clientAuth) {
        final SSLSession session = ctx.pipeline().get(SslHandler.class).engine().getSession();

        final Optional<String> customerId = getCustomerId(session.getPeerPrincipal());

        if (!customerId.isPresent()) {
          LOGGER.error("No customer ID found in certificate, closing");
          ctx.close();
          return;
        }

        ctx.channel().attr(CUSTOMER_KEY).set(customerId.get());
      }
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    LOGGER.info("New connection from: <{}>", ctx.channel().remoteAddress().toString());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // Close the connection when an exception is raised.
    if (cause instanceof NotSslRecordException) {
      LOGGER.warn("Invalid SSL/TLS record on channel, closing", cause);
    } else if (cause instanceof DecoderException) {
      LOGGER.warn("Invalid SSL/TLS record on channel, closing", cause);
    } else {
      LOGGER.error(String.format("Uncaught exception, closing (%s)", cause.getClass()), cause);
    }
    ctx.close();
  }

  /**
   * Sanitize the customer ID out of the SSL certificate principal
   *
   * @param principal Principal
   * @return customer ID or null if not found
   */
  public static Optional<String> getCustomerId(@Nullable final Principal principal) {
    if (principal == null) {
      return Optional.empty();
    }

    LOGGER.trace("Peer Principal: {}", principal);
    final String name = Strings.nullToEmpty(principal.getName());
    final Map<String, String> parts;
    try {
      parts =
          Splitter.on(',').omitEmptyStrings().trimResults().withKeyValueSeparator('=').split(name);
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Invalid certificate subject: " + name);
      return Optional.empty();
    }

    return Optional.ofNullable(parts.get("CN")).map(p -> p.toLowerCase(Locale.ENGLISH));
  }
}
