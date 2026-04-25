package com.gii.common.model.certificate;

import com.gii.common.model.common.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "certificate_templates")
public class CertificateTemplate extends BaseUuidEntity {

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "template_json", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> templateJson;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}