/**
 * Copyright 2017 Smoke Turner, LLC.
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

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;

public class UploaderConfiguration extends Configuration {

    @Valid
    @NotNull
    private final AwsConfiguration aws = new AwsConfiguration();

    @Valid
    @NotNull
    private final NettyConfiguration netty = new NettyConfiguration();

    @JsonProperty
    public AwsConfiguration getAws() {
        return aws;
    }

    @JsonProperty
    public NettyConfiguration getNetty() {
        return netty;
    }
}
