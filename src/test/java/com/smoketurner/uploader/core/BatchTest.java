/*
 * Copyright © 2019 Smoke Turner, LLC (github@smoketurner.com)
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.junit.Before;
import org.junit.Test;

public class BatchTest {

  private Batch batch;

  @Before
  public void setUp() throws Exception {
    batch = Batch.builder("test").withCreatedAt(Instant.parse("2016-12-14T16:52:13Z")).build();
  }

  @Test
  public void testAdd() throws Exception {
    batch.add("test".getBytes(StandardCharsets.UTF_8));

    assertThat(batch.getCustomerId().get()).isEqualTo("test");
    assertThat(batch.getCount()).isEqualTo(1);
    assertThat(batch.size()).isEqualTo(20);
    assertThat(batch.isFinished()).isFalse();
    assertThat(batch.isEmpty()).isFalse();
  }

  @Test
  public void testFinish() throws Exception {
    assertThat(batch.isFinished()).isFalse();
    batch.add("test".getBytes(StandardCharsets.UTF_8));
    batch.finish();
    assertThat(batch.isFinished()).isTrue();
  }

  @Test
  public void testGetInputStream() throws Exception {
    batch.add("test1".getBytes(StandardCharsets.UTF_8));
    batch.add("test2".getBytes(StandardCharsets.UTF_8));
    batch.add("test3".getBytes(StandardCharsets.UTF_8));

    final String actual =
        new BufferedReader(
                new InputStreamReader(
                    new GZIPInputStream(new ByteArrayInputStream(batch.toByteArray())),
                    StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining("\n"));

    assertThat(actual).isEqualTo("test1\ntest2\ntest3");
  }

  @Test
  public void testGetHash() {
    assertThat(Batch.getHash("test", 1)).isEqualTo("9");
  }

  @Test
  public void testGetKey() {
    final String expected = "test/e-2016/12/14/16/52/13/events_1481734333000.log.gz";
    final String actual = batch.getKey();
    assertThat(actual).isEqualTo(expected);
  }
}
