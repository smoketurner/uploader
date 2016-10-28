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
package com.smoketurner.uploader.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.smoketurner.uploader.config.AwsConfiguration;

public class UploaderTest {

    private static final DateTime NOW = DateTime.parse("2016-12-14T16:52:13Z");
    private final TransferManager s3 = mock(TransferManager.class);
    private final AwsConfiguration configuration = new AwsConfiguration();
    private final Uploader uploader = new Uploader(configuration) {
        @Override
        public DateTime now() {
            return NOW;
        }

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
    public void testGetHash() {
        assertThat(Uploader.getHash("test")).isEqualTo("9");
    }

    @Test
    public void testGenerateKey() {
        final String expected = "e-2016/12/14/16/52/13/events_1481734333000.log.gz";
        final String actual = uploader.generateKey();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testGenerateKeyPrefix() {
        configuration.setPrefix("events");
        final String expected = "events/e-2016/12/14/16/52/13/events_1481734333000.log.gz";
        final String actual = uploader.generateKey();
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void testNanoTime() {
        assertThat(uploader.nanoTime()).isEqualTo(10000L);
    }

    @Test
    public void testNow() {
        assertThat(uploader.now()).isEqualTo(NOW);
    }
}
