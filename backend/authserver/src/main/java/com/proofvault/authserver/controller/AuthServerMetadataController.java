package com.proofvault.authserver.controller;

import com.proofvault.authserver.config.AuthServerProperties;
import java.util.List;
import java.util.Map;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/authserver")
public class AuthServerMetadataController {

  private final AuthServerProperties properties;
  private final BuildProperties buildProperties;

  public AuthServerMetadataController(AuthServerProperties properties, java.util.Optional<BuildProperties> buildProperties) {
    this.properties = properties;
    this.buildProperties = buildProperties.orElse(null);
  }

  @GetMapping("/metadata")
  public Map<String, Object> metadata() {
    return Map.of(
      "service", "proofvault-authserver",
      "version", buildProperties == null ? "local" : buildProperties.getVersion(),
      "issuer", properties.issuer(),
      "audience", properties.audience(),
      "discoveryUrl", properties.issuer() + "/.well-known/openid-configuration",
      "supportedScopes", List.of("openid", "profile", "email", "proof:read", "proof:write")
    );
  }
}
