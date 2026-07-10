/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.domain.enums;

/**
 * Lifecycle of a journal entry. {@code POSTED} affects the posted balance; {@code PENDING}
 * awaits external settlement (FR-22); {@code REVERSED} is derived when a reversing entry
 * exists (the original is never mutated).
 */
public enum EntryStatus {
    POSTED,
    PENDING,
    REVERSED
}
