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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.smoketurner.uploader.application.core.Batch;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class BatchHandler extends SimpleChannelInboundHandler<byte[]> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(BatchHandler.class);
    private final long maxUploadBytes;
    private final Meter eventMeter;
    private Batch batch;

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
    public void channelRead0(ChannelHandlerContext ctx, byte[] msg)
            throws Exception {

        LOGGER.debug("Received message");

        eventMeter.mark();

        // first batch after startup
        if (batch == null) {
            batch = new Batch();
        } else if (batch.isFinished()) {
            ctx.fireChannelRead(batch);
            batch = new Batch();
        }

        batch.add(msg);

        if (batch.size() > maxUploadBytes) {
            LOGGER.debug("Batch size {} exceeds max upload size of {}",
                    batch.size(), maxUploadBytes);

            batch.finish();
            ctx.fireChannelRead(batch);
            batch = new Batch();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (batch != null && !batch.isEmpty()) {
            LOGGER.debug("Channel inactive, sending remaining batch of {}",
                    batch.getCount());
            if (!batch.isFinished()) {
                batch.finish();
            }
            ctx.fireChannelRead(batch);
            batch = new Batch();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {
        if (evt instanceof IdleStateEvent) {
            final IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                LOGGER.debug("No data received on channel, closing");
                ctx.close();
            }
        }
    }
}
