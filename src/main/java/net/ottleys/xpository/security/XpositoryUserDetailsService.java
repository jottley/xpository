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

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import net.ottleys.xpository.domain.Group;
import net.ottleys.xpository.domain.User;
import net.ottleys.xpository.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService implementation for xPository.
 *
 * <p>Loads user authentication and authorization details from the database. Maps xPository User
 * entities to Spring Security UserDetails. Group memberships are converted to granted authorities
 * with "ROLE_" prefix.
 */
@Service
public class XpositoryUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  /**
   * Constructs a XpositoryUserDetailsService.
   *
   * @param userRepository repository for user data access
   */
  public XpositoryUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Loads user details by username for authentication.
   *
   * <p>Fetches the user with groups eagerly loaded to avoid lazy loading issues during
   * authentication. Converts group memberships to granted authorities.
   *
   * @param username the username identifying the user
   * @return UserDetails for authentication
   * @throws UsernameNotFoundException if user not found or account is invalid
   */
  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByUsernameWithGroups(username)
            .orElseThrow(
                () -> new UsernameNotFoundException("User not found with username: " + username));

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPasswordHash() != null ? user.getPasswordHash() : "",
        user.isEnabled(),
        !user.isAccountExpired(),
        !user.isCredentialsExpired(),
        !user.isAccountLocked(),
        getAuthorities(user));
  }

  /**
   * Converts user's group memberships to granted authorities.
   *
   * <p>Each group is mapped to a "ROLE_" authority. For example, a user in the "admin" group
   * receives the "ROLE_ADMIN" authority.
   *
   * @param user the user
   * @return collection of granted authorities
   */
  private Collection<? extends GrantedAuthority> getAuthorities(User user) {
    Collection<GrantedAuthority> authorities = new ArrayList<>();

    // Add authorities from group memberships
    authorities.addAll(
        user.getGroups().stream()
            .map(Group::getName)
            .map(name -> "ROLE_" + name.toUpperCase().replace(" ", "_"))
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList()));

    // If no groups, add default USER role
    if (authorities.isEmpty()) {
      authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    }

    return authorities;
  }
}
