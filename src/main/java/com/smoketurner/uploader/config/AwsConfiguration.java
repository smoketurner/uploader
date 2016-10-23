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

import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.valuehandling.UnwrapValidatedValue;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.AmazonS3Client;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.net.HostAndPort;
import com.smoketurner.uploader.managed.AmazonS3ClientManager;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Size;
import io.dropwizard.util.SizeUnit;
import io.dropwizard.validation.MaxSize;
import io.dropwizard.validation.MinSize;

public class AwsConfiguration {

    @NotEmpty
    private String bucketName;

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
        this.bucketName = bucketName;
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
    public void setProxy(final Optional<HostAndPort> proxy) {
        this.proxy = proxy;
    }

    @JsonProperty
    public Size getMaxUploadSize() {
        return maxUploadSize;
    }

    @JsonProperty
    public void setMaxUploadSize(Size maxUploadSize) {
        this.maxUploadSize = maxUploadSize;
    }

    @JsonIgnore
    public ClientConfiguration getClientConfiguration() {
        final ClientConfiguration clientConfig = new ClientConfiguration();
        if (proxy.isPresent()) {
            clientConfig.setProxyHost(proxy.get().getHostText());
            clientConfig.setProxyPort(proxy.get().getPort());
        }
        clientConfig.setUseTcpKeepAlive(true);
        clientConfig.setUseGzip(true);
        return clientConfig;
    }

    public AmazonS3Client buildS3(final Environment environment) {
        final ClientConfiguration clientConfig = getClientConfiguration();
        final AmazonS3Client s3 = new AmazonS3Client(clientConfig);
        environment.lifecycle().manage(new AmazonS3ClientManager(s3));
        return s3;
    }
}
