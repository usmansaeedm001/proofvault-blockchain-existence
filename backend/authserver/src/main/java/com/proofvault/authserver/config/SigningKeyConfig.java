package com.proofvault.authserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SigningKeyConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(SigningKeyConfig.class);

  @Bean
  JWKSource<SecurityContext> jwkSource(AuthServerProperties properties) {
    RSAKey rsaKey = loadConfiguredKey(properties.signing());
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (selector, context) -> selector.select(jwkSet);
  }

  private RSAKey loadConfiguredKey(AuthServerProperties.Signing signing) {
    boolean hasPrivateKey = signing.privateKeyPem() != null && !signing.privateKeyPem().isBlank();
    boolean hasPublicKey = signing.publicKeyPem() != null && !signing.publicKeyPem().isBlank();
    if (hasPrivateKey && hasPublicKey) {
      try {
        return new RSAKey.Builder(parsePublicKey(signing.publicKeyPem()))
          .privateKey(parsePrivateKey(signing.privateKeyPem()))
          .keyID(UUID.nameUUIDFromBytes(signing.publicKeyPem().getBytes()).toString())
          .build();
      } catch (Exception exception) {
        throw new IllegalStateException("Invalid RSA signing key configuration.", exception);
      }
    }

    if (signing.requireConfiguredKey()) {
      throw new IllegalStateException("Production auth server requires AUTH_SIGNING_PRIVATE_KEY_PEM and AUTH_SIGNING_PUBLIC_KEY_PEM.");
    }

    LOGGER.warn("Generating an ephemeral RSA signing key. Configure persistent signing keys outside local development.");
    return generateEphemeralKey();
  }

  private RSAKey generateEphemeralKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to generate local RSA signing key.", exception);
    }
  }

  private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
    String base64 = sanitizePem(pem, "PRIVATE KEY");
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64));
    return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
  }

  private RSAPublicKey parsePublicKey(String pem) throws Exception {
    String base64 = sanitizePem(pem, "PUBLIC KEY");
    X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.getDecoder().decode(base64));
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
  }

  private String sanitizePem(String pem, String label) {
    return pem
      .replace("-----BEGIN " + label + "-----", "")
      .replace("-----END " + label + "-----", "")
      .replace("\\n", "")
      .replace("\n", "")
      .replace("\r", "")
      .replace(" ", "")
      .trim();
  }
}
