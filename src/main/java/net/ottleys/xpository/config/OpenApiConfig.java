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

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for SpringDoc OpenAPI documentation.
 *
 * <p>Provides Swagger UI at /swagger-ui.html and OpenAPI JSON at /v3/api-docs. Automatically
 * generates interactive API documentation from controller annotations, Bean Validation constraints,
 * and Spring MVC mappings.
 *
 * <p>Access the interactive documentation at: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

  /**
   * Configures the OpenAPI specification with API metadata.
   *
   * <p>Defines API version, description, contact information, license, and server URLs. This
   * metadata appears in the Swagger UI header and OpenAPI JSON output.
   *
   * @return OpenAPI configuration bean
   */
  @Bean
  public OpenAPI xpositoryOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("xPository API")
                .description(
                    "Lightweight metadata management system (MMS) providing REST APIs for managing "
                        + "collections, elements, and traits with access control and versioning capabilities.")
                .version("v1.0.0-SNAPSHOT")
                .contact(
                    new Contact()
                        .name("Jared Ottley")
                        .email("jared@ottleys.net")
                        .url("https://github.com/jottley"))
                .license(
                    new License()
                        .name("Apache License 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .externalDocs(
            new ExternalDocumentation()
                .description("xPository Design Documentation")
                .url("https://github.com/jottley/xpository/blob/master/DESIGN.md"))
        .servers(
            List.of(
                new Server().url("http://localhost:8080").description("Development server"),
                new Server()
                    .url("https://api.xpository.example.com")
                    .description("Production server (example)")));
  }
}
