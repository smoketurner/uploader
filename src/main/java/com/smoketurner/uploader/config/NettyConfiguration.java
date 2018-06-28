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
package com.smoketurner.uploader.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.util.Size;
import io.dropwizard.util.SizeUnit;
import io.dropwizard.validation.MaxSize;
import io.dropwizard.validation.MinSize;
import io.dropwizard.validation.PortRange;
import javax.annotation.Nullable;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class NettyConfiguration {

  @NotNull
  @MinSize(value = 1, unit = SizeUnit.BYTES)
  @MaxSize(value = 250, unit = SizeUnit.KILOBYTES)
  private Size maxLength = Size.kilobytes(100);

  @PortRange private int listenPort = 4433;

  private boolean ssl = false;
  private boolean selfSignedCert = false;
  private boolean clientAuth = false;

  @Nullable private String keyCertChainFile;

  @Nullable private String keyFile;

  @Nullable private String keyPassword;

  @Nullable private String trustCertCollectionFile;

  @Valid @NotNull @JsonProperty
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

  @Nullable
  @JsonProperty
  public String getKeyCertChainFile() {
    return keyCertChainFile;
  }

  @JsonProperty
  public void setKeyCertChainFile(@Nullable String filename) {
    this.keyCertChainFile = filename;
  }

  @Nullable
  @JsonProperty
  public String getKeyFile() {
    return keyFile;
  }

  @JsonProperty
  public void setKeyFile(@Nullable String filename) {
    this.keyFile = filename;
  }

  @Nullable
  @JsonProperty
  public String getKeyPassword() {
    return keyPassword;
  }

  @JsonProperty
  public void setKeyPassword(@Nullable String password) {
    this.keyPassword = password;
  }

  @Nullable
  @JsonProperty
  public String getTrustCertCollectionFile() {
    return trustCertCollectionFile;
  }

  @JsonProperty
  public void setTrustCertCollectionFile(@Nullable String filename) {
    this.trustCertCollectionFile = filename;
  }

  @JsonProperty("filters")
  public IpFilterConfiguration getIpFilters() {
    return filters;
  }
}
