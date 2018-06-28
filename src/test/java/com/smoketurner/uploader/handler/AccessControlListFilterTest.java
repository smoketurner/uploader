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
package com.smoketurner.uploader.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import io.netty.handler.ipfilter.IpSubnetFilterRule;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.Test;

public class AccessControlListFilterTest {

  @Test
  public void testGetRules() {
    final List<String> accept = Lists.newArrayList("127.0.0.1", "10.0.0.0/8");
    final List<String> reject = Lists.newArrayList("3.0.0.0/8");

    final IpSubnetFilterRule[] actual = AccessControlListFilter.getRules(accept, reject);

    assertThat(actual[0].matches(newSockAddress("127.0.0.1"))).isTrue();
    assertThat(actual[1].matches(newSockAddress("10.57.30.10"))).isTrue();
    assertThat(actual[2].matches(newSockAddress("3.113.4.4"))).isTrue();
  }

  private static InetSocketAddress newSockAddress(String ipAddress) {
    return new InetSocketAddress(ipAddress, 8080);
  }
}
