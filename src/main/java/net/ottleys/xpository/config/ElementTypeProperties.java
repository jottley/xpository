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

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for element type loading and validation.
 *
 * <p>Controls how element type JSON definitions are discovered and loaded at application startup,
 * including maximum file sizes for security and location scanning paths that support both classpath
 * and filesystem resources.
 */
@Configuration
@ConfigurationProperties(prefix = "xpository.element-types")
public class ElementTypeProperties {

  /** Maximum file size for element type JSON definitions (in bytes). Default: 1MB */
  private long maxFileSize = 1_048_576;

  /**
   * Locations to scan for element type definitions. Supports classpath: and file: prefixes.
   * Processed in order - dependencies should be loaded before types that extend them.
   */
  private List<String> locations =
      List.of(
          "classpath:element-types/*.json", // OTB types
          "classpath:element-types/custom/*.json" // Customer extensions
          );

  public long getMaxFileSize() {
    return maxFileSize;
  }

  public void setMaxFileSize(long maxFileSize) {
    this.maxFileSize = maxFileSize;
  }

  public List<String> getLocations() {
    return new ArrayList<>(locations);
  }

  public void setLocations(List<String> locations) {
    this.locations = locations;
  }
}
