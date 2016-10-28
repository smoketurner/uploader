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

import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.smoketurner.uploader.core.Batch;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class BatchHandler extends SimpleChannelInboundHandler<byte[]> {

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

        final MetricRegistry registry = SharedMetricRegistries
                .getOrCreate("default");
        this.eventMeter = registry
                .meter(MetricRegistry.name(BatchHandler.class, "event-rate"));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.debug("channelActive: creating new batch");
        curBatch.set(new Batch());
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, byte[] msg)
            throws Exception {

        LOGGER.trace("channelRead0: received message");

        eventMeter.mark();

        final Batch batch = curBatch.get();
        batch.add(msg);

        if (batch.size() > maxUploadBytes) {
            LOGGER.debug(
                    "Batch size {} bytes exceeds max upload size of {} bytes",
                    batch.size(), maxUploadBytes);

            batch.finish();
            ctx.fireChannelRead(batch);
            curBatch.set(new Batch());
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
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
        } else {
            LOGGER.debug("Channel inactive, current batch is empty");
        }
        curBatch.set(null);
    }
}
