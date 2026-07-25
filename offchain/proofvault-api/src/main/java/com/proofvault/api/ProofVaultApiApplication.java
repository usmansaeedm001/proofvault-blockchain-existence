package com.proofvault.api;

import com.proofvault.api.config.ProofVaultProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ProofVaultProperties.class)
public class ProofVaultApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(ProofVaultApiApplication.class, args);
  }
}
