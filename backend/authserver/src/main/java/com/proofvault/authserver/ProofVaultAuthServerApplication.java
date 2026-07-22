package com.proofvault.authserver;

import com.proofvault.authserver.config.AuthServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuthServerProperties.class)
public class ProofVaultAuthServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(ProofVaultAuthServerApplication.class, args);
  }
}
