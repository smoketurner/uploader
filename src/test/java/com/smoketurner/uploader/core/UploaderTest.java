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
package com.smoketurner.uploader.core;

import static org.mockito.Mockito.mock;

import com.smoketurner.uploader.config.AwsConfiguration;
import org.junit.Before;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class UploaderTest {

  private final S3AsyncClient mockS3 = mock(S3AsyncClient.class);
  private final AwsConfiguration configuration = new AwsConfiguration();
  private final Uploader uploader = new Uploader(mockS3, configuration);

  @Before
  public void setUp() {
    uploader.setCurrentTimeProvider(() -> 10000L);
  }
}
