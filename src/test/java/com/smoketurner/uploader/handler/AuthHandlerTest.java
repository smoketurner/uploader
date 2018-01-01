/**
 * Copyright 2018 Smoke Turner, LLC.
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
import java.security.Principal;
import java.util.Optional;
import javax.annotation.Nullable;
import org.junit.Test;

public class AuthHandlerTest {

    @Test
    public void testNullPrincipal() {
        final Optional<String> actual = AuthHandler.getCustomerId(null);
        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void testPrincipalNullName() {
        final Optional<String> actual = AuthHandler
                .getCustomerId(new Principal() {
                    @Override
                    @Nullable
                    public String getName() {
                        return null;
                    }
                });
        assertThat(actual.isPresent()).isFalse();
    }

    @Test
    public void testCN() {
        final Optional<String> actual = AuthHandler
                .getCustomerId(new Principal() {
                    @Override
                    public String getName() {
                        return "CN=test";
                    }
                });
        assertThat(actual.get()).isEqualTo("test");
    }

    @Test
    public void testCNFull() {
        final Optional<String> actual = AuthHandler
                .getCustomerId(new Principal() {
                    @Override
                    public String getName() {
                        return "CN=client1, O=Test Corp, L=Tester, ST=Florida, C=US";
                    }
                });
        assertThat(actual.get()).isEqualTo("client1");
    }

    @Test
    public void testInvalidCN() {
        final Optional<String> actual = AuthHandler
                .getCustomerId(new Principal() {
                    @Override
                    public String getName() {
                        return "test:1; test:2";
                    }
                });
        assertThat(actual.isPresent()).isFalse();
    }
}
