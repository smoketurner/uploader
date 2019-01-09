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

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.smoketurner.uploader.core.Batch;
import com.smoketurner.uploader.core.Uploader;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Objects;

@Sharable
public final class UploadHandler extends SimpleChannelInboundHandler<Batch> {

  private final Uploader uploader;
  private final Meter batchMeter;

  /**
   * Constructor
   *
   * @param uploader AWS S3 uploader
   */
  public UploadHandler(final Uploader uploader) {
    this.uploader = Objects.requireNonNull(uploader);

    final MetricRegistry registry = SharedMetricRegistries.getDefault();
    this.batchMeter = registry.meter(MetricRegistry.name(UploadHandler.class, "batch-rate"));
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, Batch batch) throws Exception {
    batchMeter.mark();
    uploader.upload(batch);
  }
}
