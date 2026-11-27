package com.gii.common.repository.faculty;

import com.gii.common.entity.faculty.FacultyMember;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacultyMemberRepository extends JpaRepository<FacultyMember, UUID> {
  List<FacultyMember> findAllByOrderByPositionAscIdAsc();

  boolean existsByGroupId(UUID groupId);
}
