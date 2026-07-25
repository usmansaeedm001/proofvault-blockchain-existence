package com.proofvault.authserver.service;

import com.proofvault.authserver.config.AuthServerProperties;
import com.proofvault.authserver.dto.WalletAuthenticationRequest;
import com.proofvault.authserver.dto.WalletNonceRequest;
import com.proofvault.authserver.dto.WalletNonceResponse;
import com.proofvault.authserver.dto.WalletTokenResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WalletAuthenticationService {
	private static final Logger LOGGER = LoggerFactory.getLogger(WalletAuthenticationService.class);
	private static final String SCOPE = "openid profile email proof:read proof:write";
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final AuthServerProperties properties;
	private final JdbcOperations jdbcOperations;
	private final JdbcUserDetailsManager userDetailsManager;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final WalletMessageFactory messageFactory;
	private final WalletSignatureVerifier signatureVerifier;
	private final Clock clock = Clock.systemUTC();

	public WalletAuthenticationService(AuthServerProperties properties, JdbcOperations jdbcOperations, JdbcUserDetailsManager userDetailsManager,
	                                   PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder, WalletMessageFactory messageFactory,
	                                   WalletSignatureVerifier signatureVerifier) {
		this.properties = properties;
		this.jdbcOperations = jdbcOperations;
		this.userDetailsManager = userDetailsManager;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.messageFactory = messageFactory;
		this.signatureVerifier = signatureVerifier;
	}

	@Transactional
	public WalletNonceResponse createChallenge(WalletNonceRequest request) {
		ensureWalletAuthEnabled();
		String walletAddress = normalize(request.walletAddress());
		long chainId = request.chainId() == null ? properties.wallet().chainId() : request.chainId();
		LOGGER.info("Wallet challenge requested wallet={} chainId={}", shortWallet(walletAddress), chainId);
		LOGGER.debug("Wallet challenge chain validation wallet={} requestedChainId={} configuredChainId={}", shortWallet(walletAddress), chainId,
			properties.wallet().chainId());
		if (chainId != properties.wallet().chainId()) {
			LOGGER.warn("Wallet challenge rejected wallet={} reason=chain_mismatch requestedChainId={} configuredChainId={}", shortWallet(walletAddress), chainId,
				properties.wallet().chainId());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet must be connected to the configured chain.");
		}

		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plus(properties.wallet().nonceTtl());
		String nonce = newNonce();

		jdbcOperations.update("""
			INSERT INTO wallet_auth_challenges (id, wallet_address, nonce_hash, chain_id, domain, issued_at, expires_at)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""", UUID.randomUUID().toString(), walletAddress, sha256Hex(nonce), chainId, properties.wallet().domain(), timestamp(issuedAt),
			timestamp(expiresAt));

		String message = messageFactory.create(properties.wallet().domain(), properties.wallet().uri(), walletAddress, chainId, nonce, issuedAt, expiresAt);
		LOGGER.info("Wallet challenge created wallet={} chainId={} expiresAt={}", shortWallet(walletAddress), chainId, expiresAt);
		return new WalletNonceResponse(walletAddress, chainId, nonce, message, expiresAt);
	}

	@Transactional
	public WalletTokenResponse authenticate(WalletAuthenticationRequest request) {
		ensureWalletAuthEnabled();
		String walletAddress = normalize(request.walletAddress());
		long chainId = request.chainId() == null ? properties.wallet().chainId() : request.chainId();
		LOGGER.info("Wallet authentication requested wallet={} chainId={}", shortWallet(walletAddress), chainId);
		LOGGER.debug("Wallet authentication chain validation wallet={} requestedChainId={} configuredChainId={}", shortWallet(walletAddress), chainId,
			properties.wallet().chainId());
		if (chainId != properties.wallet().chainId()) {
			LOGGER.warn("Wallet authentication rejected wallet={} reason=chain_mismatch requestedChainId={} configuredChainId={}", shortWallet(walletAddress), chainId,
				properties.wallet().chainId());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet must be connected to the configured chain.");
		}

		WalletChallenge challenge = findActiveChallenge(walletAddress, request.nonce());
		if (!Objects.equals(challenge.chainId(),chainId)) {
			LOGGER.warn("Wallet authentication rejected wallet={} reason=challenge_chain_mismatch requestedChainId={} challengeChainId={}", shortWallet(walletAddress),
				chainId, challenge.chainId());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wallet challenge was issued for a different chain.");
		}
		String message = messageFactory.create(challenge.domain(), properties.wallet().uri(), walletAddress, chainId, request.nonce(), challenge.issuedAt(), challenge.expiresAt());
		if (!signatureVerifier.verify(message, request.signature(), walletAddress)) {
			LOGGER.warn("Wallet authentication rejected wallet={} reason=signature_verification_failed chainId={}", shortWallet(walletAddress), chainId);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wallet signature verification failed.");
		}

		jdbcOperations.update("UPDATE wallet_auth_challenges SET consumed_at = ? WHERE id = ?", timestamp(Instant.now(clock)), challenge.id());
		createWalletUserIfMissing(walletAddress);
		WalletTokenResponse tokenResponse = issueToken(walletAddress);
		LOGGER.info("Wallet authentication succeeded wallet={} chainId={} expiresInSeconds={}", shortWallet(walletAddress), chainId, tokenResponse.expiresIn());
		return tokenResponse;
	}

	private WalletChallenge findActiveChallenge(String walletAddress, String nonce) {
		String nonceHash = sha256Hex(nonce);
		LOGGER.debug("Looking up active wallet challenge wallet={}", shortWallet(walletAddress));
		return jdbcOperations.query("""
			SELECT id, chain_id, domain, issued_at, expires_at
			FROM wallet_auth_challenges
			WHERE wallet_address = ? AND nonce_hash = ? AND consumed_at IS NULL
			ORDER BY issued_at DESC
			LIMIT 1
			""", resultSet -> {
			if (!resultSet.next()) {
				LOGGER.warn("Wallet authentication rejected wallet={} reason=challenge_not_found_or_consumed", shortWallet(walletAddress));
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wallet challenge was not found or was already used.");
			}
			WalletChallenge challenge = new WalletChallenge(resultSet.getString("id"), resultSet.getLong("chain_id"), resultSet.getString("domain"),
				resultSet.getTimestamp("issued_at").toInstant(), resultSet.getTimestamp("expires_at").toInstant());
			if (challenge.expiresAt().isBefore(Instant.now(clock))) {
				LOGGER.warn("Wallet authentication rejected wallet={} reason=challenge_expired expiresAt={}", shortWallet(walletAddress), challenge.expiresAt());
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Wallet challenge has expired.");
			}
			LOGGER.debug("Active wallet challenge found wallet={} challengeId={} chainId={} expiresAt={}", shortWallet(walletAddress), challenge.id(), challenge.chainId(),
				challenge.expiresAt());
			return challenge;
		}, walletAddress, nonceHash);
	}

	private void createWalletUserIfMissing(String walletAddress) {
		String username = walletSubject(walletAddress);
		if (userDetailsManager.userExists(username)) {
			LOGGER.debug("Wallet user already exists wallet={}", shortWallet(walletAddress));
			return;
		}

		String unusablePassword = passwordEncoder.encode(UUID.randomUUID().toString() + UUID.randomUUID());
		userDetailsManager.createUser(User.withUsername(username).password(unusablePassword).roles("USER").build());
		LOGGER.info("Wallet user created wallet={}", shortWallet(walletAddress));
	}

	private WalletTokenResponse issueToken(String walletAddress) {
		LOGGER.debug("Issuing wallet access token wallet={}", shortWallet(walletAddress));
		Instant issuedAt = Instant.now(clock);
		Instant expiresAt = issuedAt.plus(properties.tokens().accessTokenTtl());
		String subject = walletSubject(walletAddress);
		String displayName = "Wallet " + walletAddress.substring(0, 6) + "..." + walletAddress.substring(walletAddress.length() - 4);

		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.subject(subject)
			.audience(List.of(properties.audience()))
			.issuedAt(issuedAt)
			.expiresAt(expiresAt)
			.claim("scope", SCOPE)
			.claim("client_id", "proofvault-wallet")
			.claim("auth_method", "wallet")
			.claim("wallet_address", walletAddress)
			.claim("email", walletAddress.substring(2) + "@wallet.proofvault.local")
			.claim("name", displayName)
			.build();

		String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
		return new WalletTokenResponse(accessToken, "Bearer", properties.tokens().accessTokenTtl().toSeconds(), SCOPE, walletAddress);
	}

	private void ensureWalletAuthEnabled() {
		if (!properties.wallet().enabled()) {
			LOGGER.warn("Wallet authentication rejected reason=wallet_auth_disabled");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet authentication is not enabled.");
		}
	}

	private String normalize(String walletAddress) {
		return walletAddress.toLowerCase(Locale.ROOT);
	}

	private String walletSubject(String walletAddress) {
		return "wallet:" + normalize(walletAddress);
	}

	private String newNonce() {
		byte[] bytes = new byte[24];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String sha256Hex(String value) {
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return hex.toString();
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to hash wallet nonce.", exception);
		}
	}

	private Timestamp timestamp(Instant instant) {
		return Timestamp.from(instant);
	}

	private String shortWallet(String walletAddress) {
		if (walletAddress == null || walletAddress.length() < 12) {
			return "unknown";
		}
		return walletAddress.substring(0, 6) + "..." + walletAddress.substring(walletAddress.length() - 6);
	}

	private record WalletChallenge(String id, long chainId, String domain, Instant issuedAt, Instant expiresAt) {}
}
