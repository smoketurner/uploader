/**
 * Copyright 2017 Smoke Turner, LLC.
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

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.ssl.SslHandler;

public class OptionalGzipHandler extends ByteToMessageDecoder {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(OptionalGzipHandler.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in,
            List<Object> out) throws Exception {
        // Use the first five bytes to detect gzip
        if (in.readableBytes() < 5) {
            return;
        }

        if (SslHandler.isEncrypted(in)) {
            // If the channel is encrypted, close the channel as SSL must be
            // disabled and only unencrypted connections are supported.
            LOGGER.warn(
                    "Connection is encrypted when SSL is disabled, closing");
            in.clear();
            ctx.close();
            return;
        }

        final ChannelPipeline p = ctx.pipeline();
        final int magic1 = in.getUnsignedByte(in.readerIndex());
        final int magic2 = in.getUnsignedByte(in.readerIndex() + 1);
        if (isGzip(magic1, magic2)) {
            LOGGER.debug(
                    "Channel is gzipped, replacing gzip detector with inflater");
            p.replace("gzipDetector", "inflater",
                    ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
        } else {
            LOGGER.debug("Channel is not gzipped, removing gzip detector");
            p.remove(this);
        }
    }

    private static boolean isGzip(final int magic1, final int magic2) {
        return magic1 == 31 && magic2 == 139;
    }
}