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

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.primitives.Ints;
import com.smoketurner.uploader.config.NettyConfiguration;
import com.smoketurner.uploader.core.Uploader;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class UploadInitializer extends ChannelInitializer<SocketChannel> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(UploadInitializer.class);
    private static final int READER_IDLE_SECONDS = 60;
    private final NettyConfiguration configuration;
    private final UploadHandler uploadHandler;
    private final AuthHandler authHandler;
    private final SslContext sslCtx;
    private final long maxLength;
    private final long maxUploadBytes;

    /**
     * Constructor
     *
     * @param sslCtx
     *            SSL context
     * @param uploader
     *            AWS S3 uploader
     * @param configuration
     *            Netty configuration
     * @param maxUploadBytes
     *            Maximum size of S3 upload in bytes
     */
    public UploadInitializer(@Nullable final SslContext sslCtx,
            @Nonnull final Uploader uploader,
            @Nonnull final NettyConfiguration configuration,
            final long maxUploadBytes) {
        this.sslCtx = sslCtx;
        this.configuration = Objects.requireNonNull(configuration);
        this.maxLength = configuration.getMaxLength().toBytes();
        this.maxUploadBytes = maxUploadBytes;

        // handlers
        this.authHandler = new AuthHandler(configuration.isClientAuth());
        this.uploadHandler = new UploadHandler(
                Objects.requireNonNull(uploader));
    }

    @Override
    public void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            if (configuration.isClientAuth()) {
                LOGGER.info("SSL: enabling client mutual authentication");
                final SSLEngine engine = sslCtx.newEngine(ch.alloc());
                engine.setUseClientMode(false);
                engine.setNeedClientAuth(true);
                p.addLast("ssl", new SslHandler(engine));
            } else {
                p.addLast("ssl", sslCtx.newHandler(ch.alloc()));
            }
        }

        p.addLast("idleStateHandler",
                new IdleStateHandler(READER_IDLE_SECONDS, 0, 0));
        p.addLast("auth", authHandler);
        p.addLast("line", new LineBasedFrameDecoder(Ints.checkedCast(maxLength),
                true, true));
        p.addLast("decoder", new ByteArrayDecoder());
        p.addLast("batcher", new BatchHandler(maxUploadBytes));
        p.addLast("uploader", uploadHandler);
    }
}
