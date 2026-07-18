/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.repository;

import com.harrified.finCore.ledger.domain.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {

    // Tenant-scoped by @TenantId, so idempotency keys are unique per tenant (FR-16).
    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);

    // The reversing entry for a given original, if one exists (reversal is idempotent).
    Optional<JournalEntry> findByReversalOf(UUID reversalOf);
}
