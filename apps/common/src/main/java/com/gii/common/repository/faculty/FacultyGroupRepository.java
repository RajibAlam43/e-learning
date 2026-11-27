package com.gii.common.repository.faculty;

import com.gii.common.entity.faculty.FacultyGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyGroupRepository extends JpaRepository<FacultyGroup, UUID> {
  List<FacultyGroup> findAllByOrderByPositionAscIdAsc();
}
