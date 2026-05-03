package com.gii.common.repository.user;

import com.gii.common.entity.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  Optional<User> findByPhone(String phone);

  @Query(
      """
      SELECT DISTINCT u
      FROM User u
      LEFT JOIN FETCH u.userRoles ur
      LEFT JOIN FETCH ur.role
      WHERE u.email = :email
      """)
  Optional<User> findByEmailWithRoles(@Param("email") String email);

  @Query(
      """
      SELECT DISTINCT u
      FROM User u
      LEFT JOIN FETCH u.userRoles ur
      LEFT JOIN FETCH ur.role
      WHERE u.phone = :phone
      """)
  Optional<User> findByPhoneWithRoles(@Param("phone") String phone);

  boolean existsByEmail(String email);

  boolean existsByPhone(String phone);
}
