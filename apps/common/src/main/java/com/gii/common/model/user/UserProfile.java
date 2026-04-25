package com.gii.common.model.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale = "bn-BD";

    @Column(name = "timezone", length = 100)
    private String timezone;

    @Column(name = "bio")
    private String bio;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extra_json", columnDefinition = "jsonb")
    private Map<String, Object> extraJson;
}