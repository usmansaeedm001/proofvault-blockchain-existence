package com.proofvault.authserver.controller;

import com.proofvault.authserver.config.AuthServerProperties;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthServerLandingController {

  private final AuthServerProperties properties;

  public AuthServerLandingController(AuthServerProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/")
  public Map<String, Object> landing() {
    return Map.of(
      "service", "proofvault-authserver",
      "status", "running",
      "issuer", properties.issuer(),
      "metadataUrl", "/api/authserver/metadata",
      "openidConfigurationUrl", "/.well-known/openid-configuration"
    );
  }

  @GetMapping("/favicon.ico")
  public ResponseEntity<Void> favicon() {
    return ResponseEntity.noContent().build();
  }
}
