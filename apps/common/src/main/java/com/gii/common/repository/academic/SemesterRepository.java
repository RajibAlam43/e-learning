package com.gii.common.repository.academic;

import com.gii.common.entity.academic.Semester;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SemesterRepository extends JpaRepository<Semester, UUID> {
  List<Semester> findAllByOrderByStartsAtDesc();

  boolean existsByCodeAndIdNot(String code, UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Semester s where s.id = :id")
  Optional<Semester> findByIdForUpdate(@Param("id") UUID id);
}
