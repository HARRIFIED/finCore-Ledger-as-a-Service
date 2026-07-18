/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.snapshot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically advances balance snapshots so reads fold a small delta rather than an
 * account's whole posting history (ADR-0005). Best-effort: reads are correct even if this
 * never runs, so a failed tick just means the next read folds a little more.
 */
@Component
public class SnapshotWriter {

    private final SnapshotStore snapshotStore;
    private final long lagSeconds;

    public SnapshotWriter(SnapshotStore snapshotStore,
                          @Value("${fincore.ledger.snapshot.lag-seconds:5}") long lagSeconds) {
        this.snapshotStore = snapshotStore;
        this.lagSeconds = lagSeconds;
    }

    @Scheduled(fixedDelayString = "${fincore.ledger.snapshot.poll-interval-ms:5000}")
    public void advance() {
        snapshotStore.advanceAll(lagSeconds);
    }
}
