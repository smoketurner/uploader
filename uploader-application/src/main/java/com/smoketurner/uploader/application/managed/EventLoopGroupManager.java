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
package com.smoketurner.uploader.application.managed;

import java.util.Objects;
import javax.annotation.Nonnull;
import io.dropwizard.lifecycle.Managed;
import io.netty.channel.EventLoopGroup;

public class EventLoopGroupManager implements Managed {

    private final EventLoopGroup loop;

    /**
     * Constructor
     *
     * @param loop
     *            EventLoopGroup to manage
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
        loop.shutdownGracefully().sync();
    }
}
