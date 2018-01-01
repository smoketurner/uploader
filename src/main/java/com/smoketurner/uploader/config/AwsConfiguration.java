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
package com.smoketurner.uploader.config;

import java.util.Optional;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import com.smoketurner.uploader.managed.AmazonS3Manager;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Size;
import io.dropwizard.util.SizeUnit;
import io.dropwizard.validation.MaxSize;
import io.dropwizard.validation.MinSize;

public class AwsConfiguration {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AwsConfiguration.class);

    @NotEmpty
    private String bucketName = "";

    @NotEmpty
    private String region = Regions.DEFAULT_REGION.getName();

    @Nullable
    private String accessKey;

    @Nullable
    private String secretKey;

    @Nullable
    private String stsRoleArn;

    @NotNull
    @Valid
    @UnwrapValidatedValue(false)
    private Optional<String> prefix = Optional.empty();

    @NotNull
    @Valid
    @UnwrapValidatedValue(false)
    private Optional<HostAndPort> proxy = Optional.empty();

    @NotNull
    @MinSize(value = 1, unit = SizeUnit.KILOBYTES)
    @MaxSize(value = 50, unit = SizeUnit.MEGABYTES)
    private Size maxUploadSize = Size.megabytes(10);

    @NotNull
    @MinSize(value = 1, unit = SizeUnit.KILOBYTES)
    @MaxSize(value = 50, unit = SizeUnit.MEGABYTES)
    private Size minimumUploadPartSize = Size.megabytes(11);

    @JsonProperty
    public String getBucketName() {
        return bucketName;
    }

    @JsonProperty
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    @JsonProperty
    public String getRegion() {
        return region;
    }

    @JsonProperty
    public void setRegion(String region) {
        this.region = region;
    }

    @Nullable
    @JsonProperty
    public String getAccessKey() {
        return accessKey;
    }

    @JsonProperty
    public void setAcccessKey(@Nullable String key) {
        this.accessKey = key;
    }

    @Nullable
    @JsonProperty
    public String getSecretKey() {
        return secretKey;
    }

    @JsonProperty
    public void setSecretKey(@Nullable String key) {
        this.secretKey = key;
    }

    @Nullable
    @JsonProperty
    public String getStsRoleArn() {
        return stsRoleArn;
    }

    @JsonProperty
    public void setStsRoleArn(@Nullable String arn) {
        this.stsRoleArn = arn;
    }

    @JsonProperty
    public Optional<String> getPrefix() {
        return prefix;
    }

    @JsonProperty
    public void setPrefix(final String prefix) {
        this.prefix = Optional.ofNullable(prefix);
    }

    @JsonProperty
    public Optional<HostAndPort> getProxy() {
        return proxy;
    }

    @JsonProperty
    public void setProxy(final HostAndPort proxy) {
        this.proxy = Optional.ofNullable(proxy);
    }

    @JsonProperty
    public Size getMaxUploadSize() {
        return maxUploadSize;
    }

    @JsonProperty
    public void setMaxUploadSize(Size size) {
        this.maxUploadSize = size;
    }

    @JsonProperty
    public Size getMinimumUploadPartSize() {
        return minimumUploadPartSize;
    }

    @JsonProperty
    public void setMinimumUploadPartSize(Size size) {
        this.minimumUploadPartSize = size;
    }

    @JsonIgnore
    private ClientConfiguration getClientConfiguration() {
        final ClientConfiguration clientConfig = new ClientConfiguration();
        proxy.ifPresent(p -> {
            clientConfig.setProxyHost(p.getHost());
            clientConfig.setProxyPort(p.getPort());
        });
        clientConfig.setUseTcpKeepAlive(true);
        clientConfig.setUseGzip(true);
        return clientConfig;
    }

    @JsonIgnore
    public AWSCredentialsProvider getCredentials() {
        final AWSCredentialsProvider credentials;
        if (!Strings.isNullOrEmpty(accessKey)
                && !Strings.isNullOrEmpty(secretKey)) {
            credentials = new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(accessKey, secretKey));
        } else {
            credentials = new DefaultAWSCredentialsProviderChain();
        }

        if (Strings.isNullOrEmpty(stsRoleArn)) {
            return credentials;
        }

        final AWSSecurityTokenServiceClientBuilder builder = AWSSecurityTokenServiceClientBuilder
                .standard().withCredentials(credentials)
                .withClientConfiguration(getClientConfiguration());

        if (!Strings.isNullOrEmpty(region)) {
            builder.withRegion(region);
        }

        final AWSSecurityTokenService stsClient = builder.build();

        return new STSAssumeRoleSessionCredentialsProvider.Builder(stsRoleArn,
                "uploader").withStsClient(stsClient).build();
    }

    @JsonIgnore
    public AmazonS3 buildS3(final Environment environment) {
        final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(getCredentials())
                .withClientConfiguration(getClientConfiguration());

        if (!Strings.isNullOrEmpty(this.region)) {
            final Region region = Region
                    .getRegion(Regions.fromName(this.region));

            Preconditions.checkArgument(region.isServiceSupported("s3"),
                    "S3 is not supported in " + region);

            LOGGER.info("Using AWS S3 region: {}", region);

            builder.withRegion(this.region);
        }

        final AmazonS3 s3 = builder.build();
        environment.lifecycle().manage(new AmazonS3Manager(s3));
        return s3;
    }
}
