/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.repository;

import com.harrified.finCore.transfer.domain.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// Queries are automatically tenant-scoped via Hibernate @TenantId (platform-tenancy).
public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    /** Idempotent replay lookup (FR-16) — the tenant filter is applied automatically. */
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
