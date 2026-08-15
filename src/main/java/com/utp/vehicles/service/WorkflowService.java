package com.utp.vehicles.service;

import com.utp.vehicles.generated.model.WorkflowEntry;
import com.utp.vehicles.model.entity.Status;
import com.utp.vehicles.model.entity.WorkflowStep;
import com.utp.vehicles.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WorkflowService {

  private final StatusRepository statusRepository;

  public Mono<List<WorkflowEntry>> toEntries(Flux<? extends WorkflowStep> steps) {
    return steps.collectList().flatMap(this::toEntries);
  }

  private Mono<List<WorkflowEntry>> toEntries(List<? extends WorkflowStep> steps) {
    if (steps.isEmpty()) {
      return Mono.just(List.of());
    }

    Set<Integer> statusIds = steps.stream()
        .map(WorkflowStep::getIdStatus).collect(Collectors.toSet());

    return statusRepository.findAllById(statusIds)
        .collectMap(Status::getIdStatus)
        .map(statuses -> steps.stream().map(step -> toEntry(step, statuses)).toList());
  }

  private WorkflowEntry toEntry(WorkflowStep step, Map<Integer, Status> statuses) {
    Status status = statuses.get(step.getIdStatus());

    return new WorkflowEntry()
        .status(status == null ? null : status.getNameStatus())
        .dateStatusChange(step.getDateStatusChange() == null
            ? null : step.getDateStatusChange().toString())
        .observation(step.getObservation());
  }
}
