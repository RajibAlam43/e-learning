package com.gii.api.service.academic;

import com.gii.api.model.request.admin.SaveProgramCurriculumRequest;
import com.gii.api.model.request.admin.SetCollectionCoursesRequest;
import com.gii.api.model.response.admin.AdminCollectionDetailResponse;
import com.gii.api.service.admin.AdminCollectionManagementService;
import com.gii.common.repository.collection.CollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ProgramCurriculumService {
  private final CollectionRepository collections;
  private final AdminCollectionManagementService management;

  @Transactional
  public AdminCollectionDetailResponse save(
      java.util.UUID id, SaveProgramCurriculumRequest request) {
    var c =
        collections
            .findByIdForUpdate(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Program not found"));
    c.setRequiredCredits(request.requiredCredits());
    c.setElectiveCreditLimit(request.electiveCreditLimit());
    c.setExpectedDurationMonths(request.expectedDurationMonths());
    c.setAccessDurationMonths(request.accessDurationMonths());
    return management.setCourses(id, new SetCollectionCoursesRequest(request.items()));
  }
}
