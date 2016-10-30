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
package com.smoketurner.uploader.config;

import javax.annotation.Nonnull;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.uploader.core.Uploader;
import com.smoketurner.uploader.handler.UploadInitializer;
import com.smoketurner.uploader.managed.ChannelFutureManager;
import com.smoketurner.uploader.managed.EventLoopGroupManager;
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

public class NettyConfiguration {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(NettyConfiguration.class);

    @NotNull
    @MinSize(value = 1, unit = SizeUnit.BYTES)
    @MaxSize(value = 250, unit = SizeUnit.KILOBYTES)
    private Size maxLength = Size.kilobytes(100);

    @PortRange
    private int listenPort = 8888;

    private boolean ssl = false;
    private boolean selfSignedCert = false;
    private boolean clientAuth = false;
    private String keyCertChainFile;
    private String keyFile;
    private String keyPassword;
    private String trustCertCollectionFile;

    @Valid
    @NotNull
    @JsonProperty
    private final IpFilterConfiguration filters = new IpFilterConfiguration();

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

    @JsonProperty
    public boolean isSsl() {
        return ssl;
    }

    @JsonProperty
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    @JsonProperty
    public boolean isSelfSignedCert() {
        return selfSignedCert;
    }

    @JsonProperty
    public void setSelfSignedCert(boolean selfSignedCert) {
        this.selfSignedCert = selfSignedCert;
    }

    @JsonProperty
    public boolean isClientAuth() {
        return clientAuth;
    }

    @JsonProperty
    public void setClientAuth(boolean clientAuth) {
        this.clientAuth = clientAuth;
    }

    @JsonProperty
    public String getKeyCertChainFile() {
        return keyCertChainFile;
    }

    @JsonProperty
    public void setKeyCertChainFile(String filename) {
        this.keyCertChainFile = filename;
    }

    @JsonProperty
    public String getKeyFile() {
        return keyFile;
    }

    @JsonProperty
    public void setKeyFile(String filename) {
        this.keyFile = filename;
    }

    @JsonProperty
    public String getKeyPassword() {
        return keyPassword;
    }

    @JsonProperty
    public void setKeyPassword(String password) {
        this.keyPassword = password;
    }

    @JsonProperty
    public String getTrustCertCollectionFile() {
        return trustCertCollectionFile;
    }

    @JsonProperty
    public void setTrustCertCollectionFile(String filename) {
        this.trustCertCollectionFile = filename;
    }

    @JsonProperty
    public IpFilterConfiguration getIpFilters() {
        return filters;
    }

    @JsonIgnore
    public ChannelFuture build(@Nonnull final Environment environment,
            @Nonnull final Uploader uploader,
            @Nonnull final Size maxUploadSize) {

        final UploadInitializer initializer = new UploadInitializer(this,
                uploader, maxUploadSize.toBytes());

        final EventLoopGroup bossGroup;
        final EventLoopGroup workerGroup;
        if (Epoll.isAvailable()) {
            LOGGER.info("Event Loop: epoll");
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
        } else {
            LOGGER.info("Event Loop: NIO");
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
        }

        environment.lifecycle().manage(new EventLoopGroupManager(bossGroup));
        environment.lifecycle().manage(new EventLoopGroupManager(workerGroup));

        final ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.SO_BACKLOG, 128)
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
        return future;
    }
}
