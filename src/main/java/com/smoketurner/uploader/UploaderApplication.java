/**
 * Copyright 2018 Smoke Turner, LLC.
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
package com.smoketurner.uploader;

import javax.annotation.Nonnull;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.smoketurner.uploader.config.AwsConfiguration;
import com.smoketurner.uploader.config.UploaderConfiguration;
import com.smoketurner.uploader.core.NettyExecutorFactory;
import com.smoketurner.uploader.core.Uploader;
import com.smoketurner.uploader.health.AmazonS3HealthCheck;
import com.smoketurner.uploader.resources.BatchResource;
import com.smoketurner.uploader.resources.PingResource;
import com.smoketurner.uploader.resources.VersionResource;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.netty.channel.ChannelFuture;

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
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));
    }

    @Override
    public void run(@Nonnull final UploaderConfiguration configuration,
            @Nonnull final Environment environment) throws Exception {

        // set up S3 client
        final AwsConfiguration awsConfig = configuration.getAws();
        final AmazonS3 s3 = awsConfig.buildS3(environment);
        final Uploader uploader = new Uploader(awsConfig);

        // Configure the Netty TCP server
        final ChannelFuture future = configuration.getNetty().build(environment,
                uploader, awsConfig.getMaxUploadSize());

        // Configure the transfer manager to use the Netty event loop
        final TransferManager transfer = TransferManagerBuilder.standard()
                .withS3Client(s3)
                .withMinimumUploadPartSize(
                        awsConfig.getMinimumUploadPartSize().toBytes())
                .withShutDownThreadPools(false)
                .withExecutorFactory(new NettyExecutorFactory(future)).build();

        uploader.setTransferManager(transfer);

        environment.healthChecks().register("s3", new AmazonS3HealthCheck(s3));

        // Resources
        environment.jersey().register(new BatchResource(uploader));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
