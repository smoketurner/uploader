/**
 * Copyright 2018 Smoke Turner, LLC.
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.smoketurner.uploader.core.Batch;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public final class BatchHandler extends SimpleChannelInboundHandler<byte[]> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BatchHandler.class);

    private final AtomicReference<Batch> curBatch = new AtomicReference<>();
    private final long maxUploadBytes;

    // metrics
    private final Meter eventMeter;

    /**
     * Constructor
     *
     * @param maxUploadBytes
     *            Maximum size of AWS S3 file to upload
     */
    public BatchHandler(final long maxUploadBytes) {
        this.maxUploadBytes = maxUploadBytes;

        final MetricRegistry registry = SharedMetricRegistries.getDefault();
        this.eventMeter = registry
                .meter(MetricRegistry.name(BatchHandler.class, "event-rate"));
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, byte[] msg)
            throws Exception {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("channelRead0: {}",
                    new String(msg, StandardCharsets.UTF_8));
        }

        eventMeter.mark();

        final Batch batch = getBatch(ctx);
        if (batch == null) {
            LOGGER.warn("channelRead0: batch is null");
            return;
        }

        batch.add(msg);

        if (batch.size() > maxUploadBytes) {
            LOGGER.debug(
                    "Batch size {} bytes exceeds max upload size of {} bytes",
                    batch.size(), maxUploadBytes);

            batch.finish();
            ctx.fireChannelRead(batch);
            curBatch.set(newBatch(ctx));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        final Batch batch = curBatch.get();
        if (batch != null && !batch.isEmpty()) {
            LOGGER.debug(
                    "Channel inactive, sending remaining batch of {} events",
                    batch.getCount());
            batch.finish();
            ctx.fireChannelRead(batch);
        } else if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Channel inactive, current batch is empty");
        }
        curBatch.set(null);
    }

    @Nullable
    private Batch getBatch(final ChannelHandlerContext ctx) throws IOException {
        final Batch batch = curBatch.get();
        if (batch != null) {
            return batch;
        }

        final Batch newBatch = newBatch(ctx);
        if (curBatch.compareAndSet(null, newBatch)) {
            return newBatch;
        }
        return curBatch.get();
    }

    private Batch newBatch(final ChannelHandlerContext ctx) throws IOException {
        final String customerId = ctx.channel().attr(AuthHandler.CUSTOMER_KEY)
                .get();
        LOGGER.debug("Creating new batch for: {}", customerId);
        return Batch.builder(customerId).withSize(maxUploadBytes).build();
    }
}
