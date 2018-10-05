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
package com.smoketurner.uploader;

import com.smoketurner.uploader.config.AwsConfiguration;
import com.smoketurner.uploader.config.Netty;
import com.smoketurner.uploader.config.NettyConfiguration;
import com.smoketurner.uploader.config.UploaderConfiguration;
import com.smoketurner.uploader.core.Uploader;
import com.smoketurner.uploader.handler.UploadInitializer;
import com.smoketurner.uploader.managed.ChannelFutureManager;
import com.smoketurner.uploader.managed.EventLoopGroupManager;
import com.smoketurner.uploader.resources.BatchResource;
import com.smoketurner.uploader.resources.PingResource;
import com.smoketurner.uploader.resources.VersionResource;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.lifecycle.AutoCloseableManager;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Size;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import software.amazon.awssdk.core.client.builder.ClientAsyncHttpConfiguration;
import software.amazon.awssdk.http.nio.netty.EventLoopGroupConfiguration;
import software.amazon.awssdk.http.nio.netty.NettySdkHttpClientFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class UploaderApplication extends Application<UploaderConfiguration> {

  public static void main(final String[] args) throws Exception {
    java.security.Security.setProperty("networkaddress.cache.ttl", "60");
    new UploaderApplication().run(args);
  }

  @Override
  public String getName() {
    return "uploader";
  }

  @Override
  public void initialize(final Bootstrap<UploaderConfiguration> bootstrap) {
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  @Override
  public void run(final UploaderConfiguration configuration, final Environment environment)
      throws Exception {

    final NettyConfiguration nettyConfig = configuration.getNetty();
    final AwsConfiguration awsConfig = configuration.getAws();

    // we create the event loop groups first so we can share them between
    // the Netty server receiving the requests and the AWS S3 client
    // uploading the batches to S3.
    final EventLoopGroup bossGroup = Netty.newBossEventLoopGroup();
    final EventLoopGroup workerGroup = Netty.newWorkerEventLoopGroup();

    environment.lifecycle().manage(new EventLoopGroupManager(bossGroup));
    environment.lifecycle().manage(new EventLoopGroupManager(workerGroup));

    final Size maxUploadSize = awsConfig.getMaxUploadSize();

    final EventLoopGroupConfiguration eventLoopConfig =
        EventLoopGroupConfiguration.builder().eventLoopGroup(workerGroup).build();
    final NettySdkHttpClientFactory nettyFactory =
        NettySdkHttpClientFactory.builder().eventLoopGroupConfiguration(eventLoopConfig).build();
    final ClientAsyncHttpConfiguration httpConfig =
        ClientAsyncHttpConfiguration.builder().httpClientFactory(nettyFactory).build();

    // build the asynchronous S3 client with the configured credentials
    // provider and region and use the same Netty event group as the server.
    final S3AsyncClient s3 =
        S3AsyncClient.builder()
            .credentialsProvider(awsConfig.getCredentials())
            .region(awsConfig.getRegion())
            .asyncHttpConfiguration(httpConfig)
            .build();
    environment.lifecycle().manage(new AutoCloseableManager(s3));

    final Uploader uploader = new Uploader(s3, awsConfig);

    final UploadInitializer initializer =
        new UploadInitializer(nettyConfig, uploader, maxUploadSize.toBytes());

    final ServerBootstrap bootstrap = new ServerBootstrap();

    // Start the server
    final ChannelFuture future =
        bootstrap
            .group(bossGroup, workerGroup)
            .handler(new LoggingHandler(LogLevel.INFO))
            .option(ChannelOption.SO_BACKLOG, 128)
            .channel(Netty.serverChannelType())
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childHandler(initializer)
            .bind(nettyConfig.getListenPort());

    environment.lifecycle().manage(new ChannelFutureManager(future));

    // Resources
    environment.jersey().register(new BatchResource(uploader));
    environment.jersey().register(new PingResource());
    environment.jersey().register(new VersionResource());
  }
}
