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

import java.net.InetSocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
public class AuthHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AuthHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        final InetSocketAddress remote = (InetSocketAddress) ctx.channel()
                .remoteAddress();
        if (remote != null) {
            LOGGER.info("New connection from: <{}:{}>", remote.getAddress(),
                    remote.getPort());
        }
    }
}
