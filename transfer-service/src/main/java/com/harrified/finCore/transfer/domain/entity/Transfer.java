/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.domain.entity;

import com.harrified.finCore.transfer.domain.enums.TransferStatus;
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
import java.util.UUID;

/**
 * An internal transfer between two customer accounts. Records intent + outcome; the actual
 * double-entry lives in ledger-core, linked by {@code ledgerEntryId}. {@code tenantId} is
 * Hibernate's {@link TenantId} discriminator (row-level multi-tenancy, ADR-0009).
 */
@Entity
@Table(name = "transfers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "from_account_id", nullable = false)
    private UUID fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private UUID toAccountId;

    @Column(name = "from_ledger_account_id", nullable = false)
    private UUID fromLedgerAccountId;

    @Column(name = "to_ledger_account_id", nullable = false)
    private UUID toLedgerAccountId;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private TransferStatus status;

    /** Client idempotency key (FR-16); unique per tenant. */
    @Column(name = "idempotency_key", length = 200, nullable = false)
    private String idempotencyKey;

    /** The journal entry ledger-core created for this transfer. */
    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "reference", length = 255)
    private String reference;

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
