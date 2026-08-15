package com.utp.vehicles.service;

import com.utp.vehicles.client.portal.PortalServiceClient;
import com.utp.vehicles.repository.VehicleUnassignmentRequestRepository;
import com.utp.vehicles.util.Constants;
import com.utp.vehicles.util.error.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class AcceptorSelector {

  private final PortalServiceClient portalServiceClient;
  private final VehicleUnassignmentRequestRepository unassignmentRepository;

  public Mono<Integer> selectLeastLoaded(Long idCampus) {
    return portalServiceClient.getEligibleAcceptors(idCampus)
        .flatMap(acceptor -> unassignmentRepository
            .countByIdAcceptorAndIdStatus(acceptor.getIdUser().intValue(),
                Constants.ID_STATUS_IN_REVISION)
            .map(count -> Tuples.of(acceptor.getIdUser().intValue(), count)))
        .collectList()
        .flatMap(counts -> counts.stream()
            .min(Comparator.comparing(Tuple2::getT2))
            .map(Tuple2::getT1)
            .map(Mono::just)
            .orElseGet(() -> Mono.error(
                new ConflictException(Constants.ERROR_NO_ACCEPTOR_AVAILABLE))));
  }
}
