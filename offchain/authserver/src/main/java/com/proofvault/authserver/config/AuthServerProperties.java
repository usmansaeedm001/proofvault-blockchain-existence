package com.proofvault.authserver.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "proofvault.auth")
public record AuthServerProperties(@NotBlank String issuer, @NotBlank String audience, @NotEmpty List<String> allowedOrigins, @Valid Bootstrap bootstrap,
                                   @Valid Signing signing, @Valid Tokens tokens, @Valid Wallet wallet) {
	public record Bootstrap(boolean enabled, @NotBlank String userEmail, @NotBlank String userPassword, @NotBlank String clientId,
	                        @NotBlank String clientSecret, @NotBlank String publicClientId, @NotEmpty List<String> redirectUris,
	                        @NotEmpty List<String> postLogoutRedirectUris) {}

	public record Signing(boolean requireConfiguredKey, String privateKeyPem, String publicKeyPem) {}

	public record Tokens(Duration accessTokenTtl, Duration refreshTokenTtl, Duration authorizationCodeTtl) {}

	public record Wallet(boolean enabled, @Positive long chainId, @NotBlank String domain, @NotBlank String uri, @NotNull Duration nonceTtl) {}
}
