/*
 * Copyright © 2018 Smoke Turner, LLC (github@smoketurner.com)
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.net.HostAndPort;
import io.dropwizard.util.Size;
import io.dropwizard.util.SizeUnit;
import io.dropwizard.validation.MaxSize;
import io.dropwizard.validation.MinSize;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.internal.BucketUtils;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

public class AwsConfiguration {

  @NotEmpty private String bucketName = "";

  @NotNull private Region region = Region.US_EAST_1;

  @Nullable private String accessKey;

  @Nullable private String secretKey;

  @Nullable private String stsRoleArn;

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

  @JsonProperty
  public String getBucketName() {
    return bucketName;
  }

  @JsonProperty
  public void setBucketName(String bucketName) {
    BucketUtils.isValidDnsBucketName(bucketName, true);
    this.bucketName = bucketName;
  }

  @JsonProperty
  public Region getRegion() {
    return region;
  }

  @JsonProperty
  public void setRegion(String region) {
    this.region = Region.of(region);
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

  @JsonIgnore
  public AwsCredentialsProvider getCredentials() {
    final AwsCredentialsProvider credentials;
    if (!Strings.isNullOrEmpty(accessKey) && !Strings.isNullOrEmpty(secretKey)) {
      credentials =
          StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
    } else {
      credentials = DefaultCredentialsProvider.create();
    }

    if (Strings.isNullOrEmpty(stsRoleArn)) {
      return credentials;
    }

    final StsClient stsClient =
        StsClient.builder().credentialsProvider(credentials).region(region).build();

    final AssumeRoleRequest assumeRoleRequest =
        AssumeRoleRequest.builder().roleArn(stsRoleArn).build();

    return StsAssumeRoleCredentialsProvider.builder()
        .stsClient(stsClient)
        .refreshRequest(assumeRoleRequest)
        .build();
  }
}
