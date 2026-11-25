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

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility class for Spring Security operations and context access.
 *
 * <p>Provides helper methods to extract the currently authenticated user's details, falling back to
 * a default system identity when no authentication is present or when running anonymously.
 */
public final class SecurityUtils {

  public static final String DEFAULT_SYSTEM_USER = "system";

  private SecurityUtils() {
    // Prevent instantiation of utility class
  }

  /**
   * Retrieves the current authenticated username from the Spring Security context.
   *
   * <p>Returns "system" if no security context exists, if the user is unauthenticated, or if the
   * authentication is an anonymous authentication token.
   *
   * @return the authenticated username, or "system" if unauthenticated or anonymous
   */
  public static String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      return DEFAULT_SYSTEM_USER;
    }

    String username = authentication.getName();
    return (username != null && !username.isBlank() && !username.equals("anonymousUser"))
        ? username
        : DEFAULT_SYSTEM_USER;
  }
}
