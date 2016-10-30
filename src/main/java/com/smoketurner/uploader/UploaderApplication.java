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
package com.smoketurner.uploader;

import javax.annotation.Nonnull;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration;
import com.smoketurner.uploader.config.AwsConfiguration;
import com.smoketurner.uploader.config.UploaderConfiguration;
import com.smoketurner.uploader.core.Uploader;
import com.smoketurner.uploader.health.AmazonS3HealthCheck;
import com.smoketurner.uploader.resources.PingResource;
import com.smoketurner.uploader.resources.VersionResource;
import io.dropwizard.Application;
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
    public void run(@Nonnull final UploaderConfiguration configuration,
            @Nonnull final Environment environment) throws Exception {

        // set up S3 client
        final AwsConfiguration awsConfig = configuration.getAws();
        final AmazonS3Client s3 = awsConfig.buildS3(environment);
        final Uploader uploader = new Uploader(awsConfig);

        // Configure the Netty TCP server
        final ChannelFuture future = configuration.getNetty().build(environment,
                uploader, awsConfig.getMaxUploadSize());

        final TransferManagerConfiguration transferConfig = new TransferManagerConfiguration();
        transferConfig.setMinimumUploadPartSize(
                awsConfig.getMinimumUploadPartSize().toBytes());

        // Configure the transfer manager to use the Netty event loop
        final TransferManager transfer = new TransferManager(s3,
                future.channel().eventLoop(), false);
        transfer.setConfiguration(transferConfig);
        uploader.setTransferManager(transfer);

        environment.healthChecks().register("s3", new AmazonS3HealthCheck(s3));

        // Resources
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
