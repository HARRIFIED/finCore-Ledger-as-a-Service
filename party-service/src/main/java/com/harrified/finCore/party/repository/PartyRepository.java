/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.repository;

import com.harrified.finCore.party.domain.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

// Queries are automatically tenant-scoped via Hibernate @TenantId (platform-tenancy).
public interface PartyRepository extends JpaRepository<Party, UUID> {
}
