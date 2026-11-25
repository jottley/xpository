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

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuration for JPA auditing.
 *
 * <p>Enables automatic population of createdBy, createdDate, modifiedBy, and modifiedDate fields in
 * JPA entities annotated with Spring Data auditing annotations.
 *
 * <p>The auditor is resolved from the Spring Security context. If no authenticated user is
 * available, "system" is used as the default auditor.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

  /**
   * Provides the current auditor for JPA auditing.
   *
   * <p>Resolves the current auditor from Spring Security authentication context. Falls back to
   * "system" if no authentication is present.
   *
   * @return AuditorAware implementation
   */
  @Bean
  AuditorAware<String> auditorProvider() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication == null || !authentication.isAuthenticated()) {
        return Optional.of("system");
      }

      String username = authentication.getName();
      return Optional.of(
          username != null && !username.equals("anonymousUser") ? username : "system");
    };
  }
}
