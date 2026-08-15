package com.utp.vehicles.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {

  private static final String VEHICLES_PATH = "/vehicles/**";
  private static final String UNASSIGNMENT_BY_ACCEPTOR_PATH =
      "/vehicles/unassignment-requests/acceptor/**";
  private static final String UNASSIGNMENT_RESPONSE_PATH = "/vehicles/unassignment-requests/*";
  private static final String ROLE_SAE = "ROLE_SAE";
  private static final String[] APPLICANT_AUTHORITIES = {
      "ROLE_STUDENT", "ROLE_TEACHER", "ROLE_ADMINISTRATIVE"
  };

  @Bean
  SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
    return http
        .authorizeExchange(auth -> auth
            .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .pathMatchers(HttpMethod.GET, UNASSIGNMENT_BY_ACCEPTOR_PATH).hasAuthority(ROLE_SAE)
            .pathMatchers(HttpMethod.PATCH, UNASSIGNMENT_RESPONSE_PATH).hasAuthority(ROLE_SAE)
            .pathMatchers(VEHICLES_PATH).hasAnyAuthority(APPLICANT_AUTHORITIES)
            .anyExchange().authenticated())
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .oauth2ResourceServer(oauth2 -> oauth2
            .jwt(jwt -> jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())))
        .build();
  }

  private Converter<Jwt, Mono<AbstractAuthenticationToken>> grantedAuthoritiesExtractor() {
    return jwt -> {
      List<String> roles = jwt.getClaimAsStringList("roles");
      Collection<SimpleGrantedAuthority> authorities = roles == null
          ? List.of()
          : roles.stream().map(SimpleGrantedAuthority::new).toList();
      return Mono.just(new JwtAuthenticationToken(jwt, authorities));
    };
  }
}
