/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.domain.entity;

import com.harrified.finCore.party.domain.enums.PartyStatus;
import com.harrified.finCore.party.domain.enums.PartyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * The Customer/Party master (CIF) — the account-owning entity (ADR-0006). One row per party;
 * {@code type} selects which set of type-specific fields applies (individual vs organization).
 * {@code tenantId} is Hibernate's {@link TenantId} discriminator (row-level tenancy, ADR-0009).
 *
 * <p>(Bit 1 keeps profiles as nullable columns on one table; they can be normalized into
 * separate profile tables later — see party-domain-design §9.)
 */
@Entity
@Table(name = "party")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false, updatable = false)
    private PartyType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private PartyStatus status = PartyStatus.PROSPECT;

    /** The licensee's own id for this party, for their reconciliation. */
    @Column(name = "external_ref", length = 200)
    private String externalRef;

    @Column(name = "display_name", length = 255, nullable = false)
    private String displayName;

    // ── INDIVIDUAL (retail) ──────────────────────────────────────────────────
    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "nationality", length = 3)
    private String nationality;

    // ── ORGANIZATION (corporate/SME) ─────────────────────────────────────────
    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
