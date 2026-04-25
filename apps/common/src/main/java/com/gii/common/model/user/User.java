package com.gii.common.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gii.common.model.common.BaseUuidEntity;
import com.gii.common.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseUuidEntity {

    @Column(name = "student_code", unique = true, length = 50)
    private String studentCode;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "phone", unique = true, length = 20)
    private String phone;

    @Column(name = "phone_country_code", length = 5)
    private String phoneCountryCode;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status = UserStatus.active;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;
}