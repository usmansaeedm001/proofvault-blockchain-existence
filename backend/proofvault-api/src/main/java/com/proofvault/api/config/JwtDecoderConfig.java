package com.proofvault.api.config;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@ConditionalOnProperty(prefix = "proofvault.security", name = "authentication-enabled", havingValue = "true")
public class JwtDecoderConfig {
  private final ProofVaultProperties proofVaultProperties;
  private final OAuth2ResourceServerProperties resourceServerProperties;

  public JwtDecoderConfig(
    ProofVaultProperties proofVaultProperties,
    OAuth2ResourceServerProperties resourceServerProperties
  ) {
    this.proofVaultProperties = proofVaultProperties;
    this.resourceServerProperties = resourceServerProperties;
  }

  @Bean
  JwtDecoder jwtDecoder() {
    String issuerUri = resourceServerProperties.getJwt().getIssuerUri();
    String jwkSetUri = resourceServerProperties.getJwt().getJwkSetUri();

    NimbusJwtDecoder decoder = hasText(jwkSetUri)
      ? NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build()
      : NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

    OAuth2TokenValidator<Jwt> validator = JwtValidators.createDefaultWithIssuer(issuerUri);
    decoder.setJwtValidator(token -> {
      OAuth2TokenValidatorResult issuerResult = validator.validate(token);
      OAuth2TokenValidatorResult audienceResult = validateAudience(token);
      if (issuerResult.hasErrors()) {
        return issuerResult;
      }
      return audienceResult;
    });

    return decoder;
  }

  private OAuth2TokenValidatorResult validateAudience(Jwt jwt) {
    String expectedAudience = proofVaultProperties.security().audience();
    if (!hasText(expectedAudience)) {
      return OAuth2TokenValidatorResult.success();
    }

    List<String> audiences = jwt.getAudience();
    if (audiences.contains(expectedAudience)) {
      return OAuth2TokenValidatorResult.success();
    }

    return OAuth2TokenValidatorResult.failure(new OAuth2Error(
      "invalid_token",
      "JWT audience does not include " + expectedAudience,
      null
    ));
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
