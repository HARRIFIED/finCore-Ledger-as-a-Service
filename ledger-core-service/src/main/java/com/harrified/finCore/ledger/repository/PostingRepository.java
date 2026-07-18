/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.repository;

import com.harrified.finCore.ledger.domain.entity.Posting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface PostingRepository extends JpaRepository<Posting, UUID> {

    /**
     * Fold an account's postings (of a given entry status) with {@code seq > minSeq} into
     * debit/credit totals. Balances are derived from this fold (ADR-0005): pass
     * {@code minSeq = 0} to fold everything, or a snapshot's {@code as_of_seq} to fold only
     * the delta since that snapshot.
     *
     * <p>Native query — so it must filter {@code tenant_id} explicitly: native SQL bypasses
     * Hibernate's {@code @TenantId} discriminator that scopes JPQL/HQL automatically (ADR-0009).
     */
    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN p.direction = 'DEBIT'  THEN p.amount_minor ELSE 0 END), 0)::bigint AS debits,
                   COALESCE(SUM(CASE WHEN p.direction = 'CREDIT' THEN p.amount_minor ELSE 0 END), 0)::bigint AS credits
            FROM posting p
            JOIN journal_entry e ON e.id = p.entry_id
            WHERE p.ledger_account_id = :accountId
              AND p.tenant_id = :tenantId
              AND e.status = :status
              AND p.seq > :minSeq
            """, nativeQuery = true)
    DebitCreditSum fold(@Param("accountId") UUID accountId,
                        @Param("tenantId") String tenantId,
                        @Param("status") String status,
                        @Param("minSeq") long minSeq);

    /**
     * Value-dated fold for point-in-time balances (FR-20): all postings whose entry is
     * {@code status} and effective on or before {@code asOf} ({@code value_date <= asOf}).
     *
     * <p>Deliberately a full value-dated fold — it does NOT use the booking-seq snapshots,
     * which are ordered by booking sequence, not value date (see ledger-core-design §10).
     * Native query, so {@code tenant_id} is filtered explicitly (ADR-0009).
     */
    @Query(value = """
            SELECT COALESCE(SUM(CASE WHEN p.direction = 'DEBIT'  THEN p.amount_minor ELSE 0 END), 0)::bigint AS debits,
                   COALESCE(SUM(CASE WHEN p.direction = 'CREDIT' THEN p.amount_minor ELSE 0 END), 0)::bigint AS credits
            FROM posting p
            JOIN journal_entry e ON e.id = p.entry_id
            WHERE p.ledger_account_id = :accountId
              AND p.tenant_id = :tenantId
              AND e.status = :status
              AND e.value_date <= :asOf
            """, nativeQuery = true)
    DebitCreditSum foldAsOf(@Param("accountId") UUID accountId,
                            @Param("tenantId") String tenantId,
                            @Param("status") String status,
                            @Param("asOf") LocalDate asOf);
}
