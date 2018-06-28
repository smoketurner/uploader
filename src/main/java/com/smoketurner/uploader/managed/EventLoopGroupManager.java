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
import io.netty.channel.EventLoopGroup;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

public class EventLoopGroupManager implements Managed {

  private final EventLoopGroup loop;

  /**
   * Constructor
   *
   * @param loop EventLoopGroup to manage
   */
  public EventLoopGroupManager(@Nonnull final EventLoopGroup loop) {
    this.loop = Objects.requireNonNull(loop);
  }

  @Override
  public void start() throws Exception {
    // nothing to start
  }

  @Override
  public void stop() throws Exception {
    loop.shutdownGracefully(0, 0, TimeUnit.SECONDS);
  }
}
