package com.proofvault.authserver.bootstrap;

import com.proofvault.authserver.config.AuthServerProperties;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

@Component
public class AuthServerBootstrapper implements ApplicationRunner {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthServerBootstrapper.class);
	private final AuthServerProperties properties;
	private final RegisteredClientRepository registeredClientRepository;
	private final JdbcUserDetailsManager userDetailsManager;
	private final PasswordEncoder passwordEncoder;

	public AuthServerBootstrapper(AuthServerProperties properties, RegisteredClientRepository registeredClientRepository,
	                              JdbcUserDetailsManager userDetailsManager, PasswordEncoder passwordEncoder) {
		this.properties = properties;
		this.registeredClientRepository = registeredClientRepository;
		this.userDetailsManager = userDetailsManager;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!properties.bootstrap().enabled()) {
			LOGGER.info("Auth server bootstrap is disabled.");
			return;
		}

		createUserIfMissing();
		createServiceClientIfMissing();
		createPublicSpaClientIfMissing();
	}

	private void createUserIfMissing() {
		String username = properties.bootstrap().userEmail().toLowerCase();
		if (userDetailsManager.userExists(username)) {
			return;
		}

		userDetailsManager.createUser(
			User.withUsername(username).password(passwordEncoder.encode(properties.bootstrap().userPassword())).roles("USER", "ADMIN").build());
		LOGGER.info("Created bootstrap auth user {}", username);
	}

	private void createServiceClientIfMissing() {
		String clientId = properties.bootstrap().clientId();
		if (registeredClientRepository.findByClientId(clientId) != null) {
			return;
		}

		RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId(clientId)
			.clientSecret(passwordEncoder.encode(properties.bootstrap().clientSecret()))
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
			.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
			.redirectUris(uris -> uris.addAll(properties.bootstrap().redirectUris()))
			.postLogoutRedirectUris(uris -> uris.addAll(properties.bootstrap().postLogoutRedirectUris()))
			.scope(OidcScopes.OPENID)
			.scope(OidcScopes.PROFILE)
			.scope(OidcScopes.EMAIL)
			.scope("proof:read")
			.scope("proof:write")
			.clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(true).build())
			.tokenSettings(TokenSettings.builder()
				.accessTokenTimeToLive(properties.tokens().accessTokenTtl())
				.refreshTokenTimeToLive(properties.tokens().refreshTokenTtl())
				.authorizationCodeTimeToLive(properties.tokens().authorizationCodeTtl())
				.reuseRefreshTokens(false)
				.build())
			.build();

		registeredClientRepository.save(registeredClient);
		LOGGER.info("Created bootstrap OAuth2 client {}", clientId);
	}

	private void createPublicSpaClientIfMissing() {
		String clientId = properties.bootstrap().publicClientId();
		if (registeredClientRepository.findByClientId(clientId) != null) {
			return;
		}

		RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
			.clientId(clientId)
			.clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
			.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
			.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
			.redirectUris(uris -> uris.addAll(properties.bootstrap().redirectUris()))
			.postLogoutRedirectUris(uris -> uris.addAll(properties.bootstrap().postLogoutRedirectUris()))
			.scope(OidcScopes.OPENID)
			.scope(OidcScopes.PROFILE)
			.scope(OidcScopes.EMAIL)
			.scope("proof:read")
			.scope("proof:write")
			.clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).requireProofKey(true).build())
			.tokenSettings(TokenSettings.builder()
				.accessTokenTimeToLive(properties.tokens().accessTokenTtl())
				.refreshTokenTimeToLive(properties.tokens().refreshTokenTtl())
				.authorizationCodeTimeToLive(properties.tokens().authorizationCodeTtl())
				.reuseRefreshTokens(false)
				.build())
			.build();

		registeredClientRepository.save(registeredClient);
		LOGGER.info("Created bootstrap public OAuth2 client {}", clientId);
	}
}
