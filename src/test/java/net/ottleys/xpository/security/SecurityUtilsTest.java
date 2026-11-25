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

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldReturnSystemWhenNoAuthentication() {
    SecurityContextHolder.clearContext();
    assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("system");
  }

  @Test
  void shouldReturnSystemWhenUnauthenticated() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("user", "pass");
    SecurityContextHolder.getContext().setAuthentication(auth);

    // Default UsernamePasswordAuthenticationToken(user, pass) is unauthenticated
    assertThat(auth.isAuthenticated()).isFalse();
    assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("system");
  }

  @Test
  void shouldReturnSystemWhenAnonymousToken() {
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    SecurityContextHolder.getContext().setAuthentication(anonymous);

    assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("system");
  }

  @Test
  void shouldReturnSystemWhenUsernameIsAnonymousUserString() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "anonymousUser", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("system");
  }

  @Test
  void shouldReturnSystemWhenUsernameIsBlank() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "   ", "pass", List.of(new SimpleGrantedAuthority("ROLE_USER")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("system");
  }

  @Test
  void shouldReturnAuthenticatedUsername() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            "testadmin",
            "pass",
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(SecurityUtils.getCurrentUsername()).isEqualTo("testadmin");
  }
}
