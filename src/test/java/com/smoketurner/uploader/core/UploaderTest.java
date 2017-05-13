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
package com.smoketurner.uploader.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import org.junit.Before;
import org.junit.Test;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.smoketurner.uploader.config.AwsConfiguration;

public class UploaderTest {

    private final TransferManager s3 = mock(TransferManager.class);
    private final AwsConfiguration configuration = new AwsConfiguration();
    private final MetricRegistry registry = SharedMetricRegistries
            .setDefault("default", new MetricRegistry());
    private final Uploader uploader = new Uploader(configuration) {
        @Override
        public long nanoTime() {
            return 10000L;
        }
    };

    @Before
    public void setUp() {
        uploader.setTransferManager(s3);
    }

    @Test
    public void testNanoTime() {
        assertThat(uploader.nanoTime()).isEqualTo(10000L);
    }
}
