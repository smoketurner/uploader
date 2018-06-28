/*
 * Copyright © 2018 Smoke Turner, LLC (contact@smoketurner.com)
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
package com.smoketurner.uploader.managed;

import io.dropwizard.lifecycle.Managed;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.FastThreadLocal;
import java.util.Objects;
import javax.annotation.Nonnull;

public class ChannelFutureManager implements Managed {

  private final ChannelFuture future;

  /**
   * Constructor
   *
   * @param future ChannelFuture to manage
   */
  public ChannelFutureManager(@Nonnull final ChannelFuture future) {
    this.future = Objects.requireNonNull(future);
  }

  @Override
  public void start() throws Exception {
    future.sync();
  }

  @Override
  public void stop() throws Exception {
    future.channel().closeFuture();
    // clean up internal Netty threads
    FastThreadLocal.removeAll();
    FastThreadLocal.destroy();
  }
}
