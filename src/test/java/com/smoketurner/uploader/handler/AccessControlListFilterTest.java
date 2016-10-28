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
package com.smoketurner.uploader.handler;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.google.common.collect.Lists;
import io.netty.handler.ipfilter.IpFilterRuleType;
import io.netty.handler.ipfilter.IpSubnetFilterRule;

public class AccessControlListFilterTest {

    @Test
    public void testGetRules() {
        final List<String> accept = Lists.newArrayList("127.0.0.1");
        final List<String> reject = Collections.emptyList();

        final IpSubnetFilterRule[] actual = AccessControlListFilter
                .getRules(accept, reject);

        final List<IpSubnetFilterRule> expected = new ArrayList<>();
        expected.add(new IpSubnetFilterRule("127.0.0.1", 32,
                IpFilterRuleType.ACCEPT));

        assertThat(actual).isEqualTo(expected);
    }
}
