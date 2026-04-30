package com.gii.common.repository.user;

import com.gii.common.entity.user.User;
import com.gii.common.enums.UserStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  Optional<User> findByPhone(String phone);

  Optional<User> findByStudentCode(String studentCode);

  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);

  boolean existsByStudentCode(String studentCode);

  long countByStatus(UserStatus status);
}
