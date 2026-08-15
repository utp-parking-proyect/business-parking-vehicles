package com.utp.vehicles.client.portal;

import com.utp.vehicles.generated.client.users.model.CycleResponse;
import com.utp.vehicles.generated.client.users.model.Role;
import com.utp.vehicles.generated.client.users.model.UserResponse;
import com.utp.vehicles.util.Constants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortalServiceClient {

  private final WebClient portalWebClient;

  public Mono<UserResponse> getUserById(Long id) {
    return portalWebClient.get()
        .uri("/users/{id}", id)
        .retrieve()
        .bodyToMono(UserResponse.class);
  }

  public Flux<UserResponse> getEligibleAcceptors(Long idCampus) {
    return portalWebClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/users/by-role/{roleName}")
            .queryParamIfPresent("idCampus", Optional.ofNullable(idCampus))
            .build(Constants.ROLE_NAME_SAE))
        .retrieve()
        .bodyToFlux(UserResponse.class);
  }

  public boolean hasSaeRole(UserResponse user) {
    if (user.getRoles() == null) {
      return false;
    }
    return user.getRoles().stream()
        .map(Role::getName)
        .anyMatch(Constants.ROLE_NAME_SAE::equalsIgnoreCase);
  }

  public Mono<CycleResponse> getCurrentCycle() {
    return portalWebClient.get()
        .uri("/cycles/current")
        .retrieve()
        .bodyToMono(CycleResponse.class);
  }

  public Mono<CycleResponse> getCycleById(Long id) {
    return portalWebClient.get()
        .uri("/cycles/{id}", id)
        .retrieve()
        .bodyToMono(CycleResponse.class);
  }
}
