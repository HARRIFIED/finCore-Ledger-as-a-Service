/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.snapshot;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads and advances per-account balance snapshots (ADR-0005). Uses {@link JdbcTemplate}:
 * the advance is a cross-tenant background operation, so it must not be subject to Hibernate's
 * {@code @TenantId} filtering — tenant is handled explicitly in SQL.
 */
@Component
public class SnapshotStore {

    private static final String FIND = """
            SELECT as_of_seq, debits_total, credits_total
            FROM balance_snapshot
            WHERE ledger_account_id = ? AND tenant_id = ?
            """;

    /**
     * Advance every account's snapshot by folding its POSTED postings that are newer than the
     * snapshot ({@code seq > as_of_seq}) AND old enough to be safely committed
     * ({@code created_at <= now() - lag}). The lag guarantees the folded range is contiguous
     * (no lower seq can still arrive), so {@code as_of_seq} stays a gap-free high-water mark.
     * Reads remain correct with any lag — the snapshot is purely an optimization.
     */
    private static final String ADVANCE = """
            INSERT INTO balance_snapshot (ledger_account_id, tenant_id, as_of_seq, debits_total, credits_total, as_of_time)
            SELECT p.ledger_account_id,
                   p.tenant_id,
                   MAX(p.seq),
                   COALESCE(SUM(CASE WHEN p.direction = 'DEBIT'  THEN p.amount_minor ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN p.direction = 'CREDIT' THEN p.amount_minor ELSE 0 END), 0),
                   now()
            FROM posting p
            JOIN journal_entry e ON e.id = p.entry_id
            LEFT JOIN balance_snapshot s ON s.ledger_account_id = p.ledger_account_id
            WHERE e.status = 'POSTED'
              AND p.created_at <= now() - (? * interval '1 second')
              AND p.seq > COALESCE(s.as_of_seq, 0)
            GROUP BY p.ledger_account_id, p.tenant_id
            ON CONFLICT (ledger_account_id) DO UPDATE SET
                as_of_seq     = EXCLUDED.as_of_seq,
                debits_total  = balance_snapshot.debits_total + EXCLUDED.debits_total,
                credits_total = balance_snapshot.credits_total + EXCLUDED.credits_total,
                as_of_time    = EXCLUDED.as_of_time
            """;

    private final JdbcTemplate jdbc;

    public SnapshotStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<SnapshotRow> find(UUID ledgerAccountId, String tenantId) {
        return jdbc.query(FIND,
                        (rs, n) -> new SnapshotRow(rs.getLong("as_of_seq"), rs.getLong("debits_total"), rs.getLong("credits_total")),
                        ledgerAccountId, tenantId)
                .stream().findFirst();
    }

    /** Advance all snapshots; returns the number of accounts updated/inserted. */
    public int advanceAll(long lagSeconds) {
        return jdbc.update(ADVANCE, lagSeconds);
    }

    public record SnapshotRow(long asOfSeq, long debits, long credits) {
    }
}
