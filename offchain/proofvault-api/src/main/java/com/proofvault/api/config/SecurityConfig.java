package com.proofvault.api.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private final ProofVaultProperties properties;

  public SecurityConfig(ProofVaultProperties properties) {
    this.properties = properties;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
      .csrf(AbstractHttpConfigurer::disable)
      .cors(Customizer.withDefaults())
      .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .headers(headers -> headers
        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
        .permissionsPolicyHeader(policy -> policy.policy("geolocation=(), microphone=(), camera=()"))
      );

    if (properties.security().authenticationEnabled()) {
      http
        .authorizeHttpRequests(auth -> auth
          .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
          .requestMatchers(HttpMethod.POST, "/api/proofs/verify").permitAll()
          .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
          .anyRequest().authenticated()
        )
        .oauth2ResourceServer(oauth2 -> oauth2
          .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );
    } else {
      http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    }

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(properties.cors().allowedOrigins());
    configuration.setAllowedMethods(properties.cors().allowedMethods());
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Request-Id"));
    configuration.setExposedHeaders(List.of("Location", "X-Request-Id"));
    configuration.setAllowCredentials(false);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    source.registerCorsConfiguration("/actuator/**", configuration);
    return source;
  }

  Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return jwt -> new JwtAuthenticationToken(jwt, authorities(jwt), jwt.getSubject());
  }

  private Collection<GrantedAuthority> authorities(Jwt jwt) {
    List<GrantedAuthority> authorities = new ArrayList<>();

    Object scope = jwt.getClaims().getOrDefault("scope", jwt.getClaims().get("scp"));
    if (scope instanceof String scopeString) {
      for (String value : scopeString.split(" ")) {
        if (!value.isBlank()) {
          authorities.add(new SimpleGrantedAuthority("SCOPE_" + value));
        }
      }
    } else if (scope instanceof Collection<?> scopeValues) {
      scopeValues.stream()
        .map(String::valueOf)
        .map(value -> "SCOPE_" + value)
        .map(SimpleGrantedAuthority::new)
        .forEach(authorities::add);
    }

    Object realmAccess = jwt.getClaims().get("realm_access");
    if (realmAccess instanceof Map<?, ?> access) {
      Object roles = access.get("roles");
      if (roles instanceof Collection<?> roleValues) {
        roleValues.stream()
          .map(String::valueOf)
          .map(role -> "ROLE_" + role.toUpperCase())
          .map(SimpleGrantedAuthority::new)
          .forEach(authorities::add);
      }
    }

    return authorities;
  }
}
