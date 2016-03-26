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
package com.smoketurner.uploader.application.config;

import java.io.File;
import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.uploader.application.core.Uploader;
import com.smoketurner.uploader.application.handler.UploadInitializer;
import com.smoketurner.uploader.application.managed.ChannelFutureManager;
import com.smoketurner.uploader.application.managed.EventLoopGroupManager;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Size;
import io.dropwizard.util.SizeUnit;
import io.dropwizard.validation.MaxSize;
import io.dropwizard.validation.MinSize;
import io.dropwizard.validation.PortRange;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class NettyConfiguration {

    @NotNull
    @MinSize(value = 1, unit = SizeUnit.BYTES)
    @MaxSize(value = 1, unit = SizeUnit.MEGABYTES)
    private Size maxLength = Size.kilobytes(32);

    @PortRange
    private int listenPort = 8888;

    private boolean ssl = false;
    private boolean selfSignedCert = false;

    private String keyCertChainFile;

    private String keyFile;

    @JsonProperty
    public Size getMaxLength() {
        return maxLength;
    }

    @JsonProperty
    public void setMaxLength(Size maxLength) {
        this.maxLength = maxLength;
    }

    @JsonProperty
    public int getListenPort() {
        return listenPort;
    }

    @JsonProperty
    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    @JsonProperty("ssl")
    public boolean isSsl() {
        return ssl;
    }

    @JsonProperty("ssl")
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @JsonProperty("selfSignedCert")
    public boolean isSelfSignedCert() {
        return selfSignedCert;
    }

    @JsonProperty("selfSignedCert")
    public void setSelfSignedCert(boolean selfSignedCert) {
        this.selfSignedCert = selfSignedCert;
    }

    @JsonProperty
    public String getKeyCertChainFile() {
        return keyCertChainFile;
    }

    @JsonProperty
    public void setKeyCertChainFile(String keyCertChainFile) {
        this.keyCertChainFile = keyCertChainFile;
    }

    @JsonProperty
    public String getKeyFile() {
        return keyFile;
    }

    @JsonProperty
    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    @JsonIgnore
    public void build(@Nonnull final Environment environment,
            @Nonnull final Uploader uploader, @Nonnull final Size maxUploadSize)
            throws Exception {

        // Configure SSL
        final SslContext sslCtx;
        if (ssl) {
            if (selfSignedCert) {
                final SelfSignedCertificate ssc = new SelfSignedCertificate();
                sslCtx = SslContextBuilder
                        .forServer(ssc.certificate(), ssc.privateKey()).build();
            } else {
                sslCtx = SslContextBuilder.forServer(new File(keyCertChainFile),
                        new File(keyFile)).build();
            }
        } else {
            sslCtx = null;
        }

        final UploadInitializer initializer = new UploadInitializer(sslCtx,
                uploader, maxLength.toBytes(), maxUploadSize.toBytes());

        final EventLoopGroup bossGroup;
        final EventLoopGroup workerGroup;
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
        } else {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
        }

        environment.lifecycle().manage(new EventLoopGroupManager(bossGroup));
        environment.lifecycle().manage(new EventLoopGroupManager(workerGroup));

        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.SO_BACKLOG, 100)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(initializer);

        if (Epoll.isAvailable()) {
            bootstrap.channel(EpollServerSocketChannel.class);
        } else {
            bootstrap.channel(NioServerSocketChannel.class);
        }

        // Start the server
        final ChannelFuture future = bootstrap.bind(listenPort);
        environment.lifecycle().manage(new ChannelFutureManager(future));
    }
}
