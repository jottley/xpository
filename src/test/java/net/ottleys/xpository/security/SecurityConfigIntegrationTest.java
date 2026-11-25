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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("prod")
@TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.search.backend.directory.root=./target/test-indexes",
      // or use the in-memory local-heap directory type for tests:
      "spring.jpa.properties.hibernate.search.backend.directory.type=local-heap",
      "logging.file.name=./logs/xpository.log",
      "bucket4j.enabled=false",
      "xpository.jwt.secret=dGVzdC1zZWNyZXQtd2hpY2gtaXMtMzItYnl0ZXMtbG9uZyE=",
      "xpository.jwt.expiration-ms=60000",
      "spring.datasource.url=jdbc:sqlite::memory:"
    })
class SecurityConfigIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private ObjectMapper objectMapper;

  @Value("${xpository.jwt.secret}")
  private String jwtSecret;

  @MockitoBean private XpositoryUserDetailsService userDetailsService;

  @BeforeEach
  void setUpUser() {
    var user =
        new User(
            "testuser",
            passwordEncoder.encode("testpass"),
            List.of(new SimpleGrantedAuthority("ROLE_USER")));
    when(userDetailsService.loadUserByUsername("testuser")).thenReturn(user);
  }

  @Test
  void loginEndpoint_isPublic_andReturnsTokenForValidCredentials() throws Exception {
    mvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"testuser","password":"testpass"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());
  }

  @Test
  void protectedEndpoint_requiresBearerToken() throws Exception {
    // 1. No token -> 401
    mvc.perform(get("/api/v1/collections")).andExpect(status().isUnauthorized());

    // 2. Obtain valid token
    MvcResult loginResult =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"username":"testuser","password":"testpass"}
                        """))
            .andExpect(status().isOk())
            .andReturn();

    String token =
        objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

    // 3. Valid token -> 200
    mvc.perform(get("/api/v1/collections").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void invalidToken_returns401() throws Exception {
    mvc.perform(get("/api/v1/collections").header("Authorization", "Bearer invalid.jwt.token"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredToken_returns401() throws Exception {
    JwtTokenProvider expiredProvider = new JwtTokenProvider(jwtSecret, -1_000L);
    String expiredToken = expiredProvider.createToken("testuser");

    mvc.perform(get("/api/v1/collections").header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized());
  }
}
