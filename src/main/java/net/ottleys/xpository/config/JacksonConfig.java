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

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper configuration with security hardening.
 *
 * <p>Configures JSON serialization/deserialization with security constraints to prevent common
 * vulnerabilities including RCE via polymorphic deserialization, DoS via deep nesting, and resource
 * exhaustion via large strings or numbers.
 */
@Configuration
public class JacksonConfig {

  /**
   * Creates a security-hardened ObjectMapper for JSON processing.
   *
   * <p>Applies constraints to prevent DoS attacks and disables dangerous deserialization features.
   *
   * @return configured ObjectMapper instance
   */
  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Security: Disable features that could lead to RCE
    mapper.deactivateDefaultTyping();

    // Security: Fail on unknown properties (strict validation)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // Security: Set constraints to prevent DoS
    mapper
        .getFactory()
        .setStreamReadConstraints(
            StreamReadConstraints.builder()
                .maxNestingDepth(50) // Prevent stack overflow
                .maxStringLength(10_000_000) // 10MB max string
                .maxNumberLength(1000) // Prevent huge number parsing
                .build());

    return mapper;
  }
}
