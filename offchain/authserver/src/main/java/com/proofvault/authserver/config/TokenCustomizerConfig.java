package com.proofvault.authserver.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
public class TokenCustomizerConfig {
	@Bean
	OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(AuthServerProperties properties) {
		return context -> {
			if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
				context.getClaims().audience(List.of(properties.audience()));
				context.getClaims().claim("client_id", context.getRegisteredClient().getClientId());
			}
		};
	}
}
