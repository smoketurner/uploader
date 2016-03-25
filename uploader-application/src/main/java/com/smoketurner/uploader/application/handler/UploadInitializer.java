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
package com.smoketurner.uploader.application.handler;

import java.util.Objects;
import com.google.common.primitives.Ints;
import com.smoketurner.uploader.application.core.Uploader;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.IdleStateHandler;

public class UploadInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;
    private final Uploader uploader;
    private final long maxLength;
    private final long maxUploadBytes;

    /**
     * Constructor
     *
     * @param sslCtx
     *            SSL context
     * @param uploader
     *            AWS S3 uploader
     * @param maxLength
     *            Maximum line length
     * @param maxUploadBytes
     *            Maximum size of S3 upload in bytes
     */
    public UploadInitializer(final SslContext sslCtx, final Uploader uploader,
            final long maxLength, final long maxUploadBytes) {
        this.sslCtx = sslCtx;
        this.uploader = Objects.requireNonNull(uploader);
        this.maxLength = maxLength;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Override
    public void initChannel(final SocketChannel ch) throws Exception {
        final ChannelPipeline p = ch.pipeline();
        if (sslCtx != null) {
            p.addLast("ssl", sslCtx.newHandler(ch.alloc()));
        }

        p.addLast("idleStateHandler", new IdleStateHandler(60, 0, 0));
        p.addLast("line", new LineBasedFrameDecoder(Ints.checkedCast(maxLength),
                true, true));
        p.addLast("decoder", new ByteArrayDecoder());
        p.addLast("batcher", new BatchHandler(maxUploadBytes));
        p.addLast("uploader", new UploadHandler(uploader));
    }
}
