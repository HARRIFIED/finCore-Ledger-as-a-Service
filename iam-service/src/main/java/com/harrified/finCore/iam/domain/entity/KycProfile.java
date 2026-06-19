/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.domain.entity;

import com.harrified.finCore.iam.domain.enums.KycLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "kyc_profiles")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // Sensitive PII — encrypt at rest via a JPA AttributeConverter before going to prod.
    @Column(name = "bvn", length = 11)
    private String bvn;

    // Sensitive PII — encrypt at rest via a JPA AttributeConverter before going to prod.
    @Column(name = "nin", length = 11)
    private String nin;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_level", nullable = false, length = 20)
    @Builder.Default
    private KycLevel kycLevel = KycLevel.NONE;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    // Tenant-specific KYC fields that don't fit the standard schema.
    // e.g. { "cac_number": "RC-123456", "tin": "1234567890" }
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
