/*
 * Copyright 2024-2026 Jared Ottley
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
package net.ottleys.xpository.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final String TEST_SECRET =
      Base64.getEncoder()
          .encodeToString("test-secret-which-is-32-bytes-long!!".getBytes(StandardCharsets.UTF_8));
  private static final String OTHER_SECRET =
      Base64.getEncoder()
          .encodeToString("other-secret-which-is-32-bytes-long!".getBytes(StandardCharsets.UTF_8));

  private JwtTokenProvider provider;

  @BeforeEach
  void setUp() {
    provider = new JwtTokenProvider(TEST_SECRET, 60_000L);
  }

  @Test
  void createToken_containsUsername_andIsValid() {
    String token = provider.createToken("alice");

    assertThat(token).isNotBlank();
    assertThat(provider.validateToken(token)).isTrue();
    assertThat(provider.getUsername(token)).isEqualTo("alice");
  }

  @Test
  void expiredToken_isRejected() {
    JwtTokenProvider expiredProvider = new JwtTokenProvider(TEST_SECRET, -1_000L);
    String token = expiredProvider.createToken("bob");

    assertThat(provider.validateToken(token)).isFalse();
  }

  @Test
  void malformedToken_isRejected() {
    assertThat(provider.validateToken("not.a.jwt.token")).isFalse();
  }

  @Test
  void tokenWithDifferentSignature_isRejected() {
    JwtTokenProvider other = new JwtTokenProvider(OTHER_SECRET, 60_000L);
    String token = other.createToken("charlie");

    assertThat(provider.validateToken(token)).isFalse();
  }
}
