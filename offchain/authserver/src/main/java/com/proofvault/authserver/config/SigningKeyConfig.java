package com.proofvault.authserver.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.ByteArrayOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
public class SigningKeyConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(SigningKeyConfig.class);

  @Bean
  JWKSource<SecurityContext> jwkSource(AuthServerProperties properties) {
    RSAKey rsaKey = loadConfiguredKey(properties.signing());
    JWKSet jwkSet = new JWKSet(rsaKey);
    return (selector, context) -> selector.select(jwkSet);
  }

  @Bean
  JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }

  private RSAKey loadConfiguredKey(AuthServerProperties.Signing signing) {
    boolean hasPrivateKey = signing.privateKeyPem() != null && !signing.privateKeyPem().isBlank();
    boolean hasPublicKey = signing.publicKeyPem() != null && !signing.publicKeyPem().isBlank();
    if (hasPrivateKey) {
      try {
        RSAPrivateKey privateKey = parsePrivateKey(signing.privateKeyPem());
        RSAPublicKey publicKey = loadPublicKey(signing.publicKeyPem(), privateKey, hasPublicKey);
        LOGGER.info("Loaded configured RSA signing key publicKeyConfigured={} keyId={}", hasPublicKey,
          UUID.nameUUIDFromBytes(publicKey.getEncoded()));
        return new RSAKey.Builder(publicKey)
          .privateKey(privateKey)
          .keyID(UUID.nameUUIDFromBytes(publicKey.getEncoded()).toString())
          .build();
      } catch (Exception exception) {
        if (signing.requireConfiguredKey()) {
          throw new IllegalStateException("Invalid RSA signing key configuration.", exception);
        }
        LOGGER.warn("Ignoring invalid RSA signing key configuration because configured signing keys are not required in this profile.", exception);
        return generateEphemeralKey();
      }
    }

    if (signing.requireConfiguredKey()) {
      throw new IllegalStateException("Production auth server requires AUTH_SIGNING_PRIVATE_KEY_PEM. AUTH_SIGNING_PUBLIC_KEY_PEM is optional when the public key can be derived from the private key.");
    }

    LOGGER.warn("Generating an ephemeral RSA signing key. Configure persistent signing keys outside local development.");
    return generateEphemeralKey();
  }

  private RSAKey generateEphemeralKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      LOGGER.info("Generated ephemeral RSA signing key keyType=RSA keySize=2048");
      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .keyID(UUID.randomUUID().toString())
        .build();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to generate local RSA signing key.", exception);
    }
  }

  private RSAPublicKey loadPublicKey(String publicKeyPem, RSAPrivateKey privateKey, boolean hasPublicKey) throws Exception {
    if (!hasPublicKey) {
      return derivePublicKey(privateKey);
    }
    try {
      return parsePublicKey(publicKeyPem);
    } catch (Exception exception) {
      LOGGER.warn("Ignoring invalid AUTH_SIGNING_PUBLIC_KEY_PEM and deriving the public key from AUTH_SIGNING_PRIVATE_KEY_PEM.", exception);
      return derivePublicKey(privateKey);
    }
  }

  private RSAPrivateKey parsePrivateKey(String pem) throws Exception {
    String base64 = sanitizePem(pem);
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64));
    return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
  }

  private RSAPublicKey parsePublicKey(String pem) throws Exception {
    byte[] der = Base64.getDecoder().decode(sanitizePem(pem));
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    try {
      return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(der));
    } catch (InvalidKeySpecException exception) {
      if (!pem.contains("BEGIN RSA PUBLIC KEY")) {
        throw exception;
      }
      return (RSAPublicKey) keyFactory.generatePublic(new X509EncodedKeySpec(wrapPkcs1PublicKey(der)));
    }
  }

  private RSAPublicKey derivePublicKey(RSAPrivateKey privateKey) throws Exception {
    if (!(privateKey instanceof RSAPrivateCrtKey privateCrtKey)) {
      throw new IllegalStateException("Unable to derive RSA public key from private key. Configure AUTH_SIGNING_PUBLIC_KEY_PEM.");
    }
    RSAPublicKeySpec spec = new RSAPublicKeySpec(privateCrtKey.getModulus(), privateCrtKey.getPublicExponent());
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
  }

  private byte[] wrapPkcs1PublicKey(byte[] pkcs1Der) throws Exception {
    byte[] rsaEncryptionAlgorithm = new byte[] {
      0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7,
      0x0d, 0x01, 0x01, 0x01, 0x05, 0x00
    };
    ByteArrayOutputStream bitString = new ByteArrayOutputStream();
    bitString.write(0x03);
    writeDerLength(bitString, pkcs1Der.length + 1);
    bitString.write(0x00);
    bitString.write(pkcs1Der);

    ByteArrayOutputStream sequence = new ByteArrayOutputStream();
    sequence.write(rsaEncryptionAlgorithm);
    sequence.write(bitString.toByteArray());

    ByteArrayOutputStream subjectPublicKeyInfo = new ByteArrayOutputStream();
    subjectPublicKeyInfo.write(0x30);
    writeDerLength(subjectPublicKeyInfo, sequence.size());
    subjectPublicKeyInfo.write(sequence.toByteArray());
    return subjectPublicKeyInfo.toByteArray();
  }

  private void writeDerLength(ByteArrayOutputStream output, int length) {
    if (length < 128) {
      output.write(length);
      return;
    }
    int bytesRequired = Integer.BYTES - Integer.numberOfLeadingZeros(length) / Byte.SIZE;
    output.write(0x80 | bytesRequired);
    for (int shift = (bytesRequired - 1) * Byte.SIZE; shift >= 0; shift -= Byte.SIZE) {
      output.write((length >> shift) & 0xff);
    }
  }

  private String sanitizePem(String pem) {
    return pem
      .replace("\\n", "\n")
      .replace("\\r", "\r")
      .replace("\"", "")
      .replace("'", "")
      .replaceAll("-----BEGIN [A-Z ]*KEY-----", "")
      .replaceAll("-----END [A-Z ]*KEY-----", "")
      .replaceAll("\\s", "")
      .trim();
  }
}
