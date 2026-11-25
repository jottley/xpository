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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

class JwtAuthenticationFilterTest {

  private static final String FILTER_SECRET =
      Base64.getEncoder()
          .encodeToString("filter-secret-32-bytes-long-key!!!!!".getBytes(StandardCharsets.UTF_8));

  private JwtTokenProvider tokenProvider;
  private XpositoryUserDetailsService userDetailsService;
  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    tokenProvider = new JwtTokenProvider(FILTER_SECRET, 3_600_000L);
    userDetailsService = mock(XpositoryUserDetailsService.class);
    filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilter_withValidBearerToken_setsAuthenticatedContextWithAuthorities() throws Exception {
    String token = tokenProvider.createToken("john");
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
    var user = new User("john", "", authorities);
    when(userDetailsService.loadUserByUsername("john")).thenReturn(user);

    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

    filter.doFilterInternal(request, response, chain);

    var auth = SecurityContextHolder.getContext().getAuthentication();
    assertThat(auth).isNotNull();
    assertThat(auth.getPrincipal()).isEqualTo(user);
    assertThat(auth.isAuthenticated()).isTrue();
    assertThat(auth.getAuthorities()).hasSize(1);
    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_withoutBearerHeader_passesThroughUnauthenticated() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn(null);

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }

  @Test
  void doFilter_withInvalidToken_doesNotSetAuthentication() throws Exception {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    FilterChain chain = mock(FilterChain.class);
    when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");

    filter.doFilterInternal(request, response, chain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(chain).doFilter(request, response);
  }
}
