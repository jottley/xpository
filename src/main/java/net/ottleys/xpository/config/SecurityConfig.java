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
package net.ottleys.xpository.config;

import net.ottleys.xpository.security.JwtAuthenticationFilter;
import net.ottleys.xpository.security.JwtTokenProvider;
import net.ottleys.xpository.security.XpositoryUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the xPository application.
 *
 * <p>Provides authentication and authorization configuration including:
 *
 * <ul>
 *   <li>Database-backed user authentication via UserDetailsService
 *   <li>BCrypt password encoding
 *   <li>HTTP Basic authentication for REST API
 *   <li>Method-level security with @PreAuthorize and @PostAuthorize
 *   <li>CSRF disabled for stateless REST API
 * </ul>
 *
 * <p>Development mode (dev profile) maintains permitAll access for easier testing. Production mode
 * (prod profile) enforces authentication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final JwtTokenProvider tokenProvider;
  private final XpositoryUserDetailsService userDetailsService;

  /**
   * Constructs SecurityConfig with UserDetailsService.
   *
   * @param userDetailsService service for loading user details
   */
  public SecurityConfig(
      JwtTokenProvider tokenProvider, XpositoryUserDetailsService userDetailsService) {
    this.tokenProvider = tokenProvider;
    this.userDetailsService = userDetailsService;
  }

  /**
   * Configures the Spring Security filter chain for development and testing (permitAll).
   *
   * <p>Permits all requests without authentication. CSRF disabled for REST API compatibility.
   *
   * @param http the HttpSecurity to configure
   * @return configured SecurityFilterChain
   */
  @Bean
  @Profile({"dev", "test"})
  SecurityFilterChain devFilterChain(HttpSecurity http) {
    return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable())
        .build();
  }

  /**
   * Configures the Spring Security filter chain for production (authentication required).
   *
   * <p>Enforces authentication for all endpoints except Swagger UI and actuator health. Uses HTTP
   * Basic authentication for REST API access. CSRF disabled for stateless API.
   *
   * @param http the HttpSecurity to configure
   * @return configured SecurityFilterChain
   */
  @Bean
  @Profile("prod")
  SecurityFilterChain prodFilterChain(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(auth ->
            auth.requestMatchers(
                    "/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health")
                .permitAll()
                .anyRequest()
                .authenticated())
        .httpBasic(Customizer.withDefaults()) // keep for /actuator health if needed
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm ->
            sm.sessionCreationPolicy(
                org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
        .addFilterBefore(jwtAuthFilter(), UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthFilter() {
    return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
  }

  /**
   * Provides password encoder bean using BCrypt.
   *
   * <p>BCrypt is a strong adaptive hash function designed for password storage. Uses default
   * strength (10 rounds).
   *
   * @return BCrypt password encoder
   */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * Provides authentication manager bean.
   *
   * <p>Configures DaoAuthenticationProvider with custom UserDetailsService and password encoder.
   *
   * @return authentication manager
   */
  @Bean
  AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return new ProviderManager(provider);
  }
}
