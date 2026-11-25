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
package net.ottleys.xpository.controller;

import net.ottleys.xpository.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthenticationManager authManager;
  private final JwtTokenProvider tokenProvider;

  public AuthController(AuthenticationManager authManager, JwtTokenProvider tokenProvider) {
    this.authManager = authManager;
    this.tokenProvider = tokenProvider;
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {

    authManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.username(), request.password()));

    // authentication succeeded → generate token
    String token = tokenProvider.createToken(request.username());

    return ResponseEntity.ok(new AuthResponse(token));
  }

  public record AuthRequest(String username, String password) {}

  public record AuthResponse(String token) {}
}
