/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain.entity;

import com.harrified.finCore.ledger.domain.enums.EntryStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.TenantId;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One balanced transaction — an immutable event holding two or more {@link Posting}s
 * (append-only; corrections are reversing entries, never mutations). Idempotent per tenant
 * via {@code idempotencyKey}.
 */
@Entity
@Table(name = "journal_entry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @TenantId
    @Column(name = "tenant_id", length = 100, nullable = false)
    private String tenantId;

    @Column(name = "idempotency_key", length = 200, nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private EntryStatus status;

    /** When the entry was recorded (system time). */
    @Column(name = "booking_time", nullable = false)
    private Instant bookingTime;

    /** When the entry takes economic effect (drives point-in-time and interest). */
    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "source", length = 100)
    private String source;

    /** Set on a reversing entry to the id of the entry it reverses. */
    @Column(name = "reversal_of")
    private UUID reversalOf;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Posting> postings = new ArrayList<>();

    /** Add a posting to this entry (maintains both sides of the relationship). */
    public void addPosting(Posting posting) {
        posting.setEntry(this);
        this.postings.add(posting);
    }
}
