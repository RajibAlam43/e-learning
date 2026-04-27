package com.gii.common.repository.user;

import com.gii.common.entity.user.UserRole;
import com.gii.common.entity.user.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByUserId(UUID userId);

    boolean existsByUserIdAndRoleName(UUID userId, String roleName);

    void deleteByUserIdAndRoleId(UUID userId, Long roleId);
}