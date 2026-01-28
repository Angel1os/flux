package com.angellos.shared.model;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
public class AuditableFieldsModel {

    @Column
    private UUID updatedBy;

    @Column
    private ZonedDateTime updatedAt;

    @Column
    private UUID createdBy;

    @Column
    private ZonedDateTime createdAt;
}
