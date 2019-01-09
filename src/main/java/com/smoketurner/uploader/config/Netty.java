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
package com.smoketurner.uploader.config;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Netty {

  private static final Logger LOGGER = LoggerFactory.getLogger(Netty.class);
  private static final int WORKER_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  static {
    if (Epoll.isAvailable()) {
      LOGGER.info("Event Loop: epoll");
    } else {
      LOGGER.info("Event Loop: NIO");
    }
  }

  public static EventLoopGroup newBossEventLoopGroup() {
    if (Epoll.isAvailable()) {
      return new EpollEventLoopGroup(1);
    }
    return new NioEventLoopGroup(1);
  }

  public static EventLoopGroup newWorkerEventLoopGroup() {
    if (Epoll.isAvailable()) {
      return new EpollEventLoopGroup(WORKER_THREADS);
    }
    return new NioEventLoopGroup(WORKER_THREADS);
  }

  public static Class<? extends ServerChannel> serverChannelType() {
    if (Epoll.isAvailable()) {
      return EpollServerSocketChannel.class;
    }
    return NioServerSocketChannel.class;
  }
}
