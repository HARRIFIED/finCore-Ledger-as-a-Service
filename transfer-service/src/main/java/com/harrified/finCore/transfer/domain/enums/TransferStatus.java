/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.transfer.domain.enums;

/** Lifecycle of an internal transfer. External/async states (PENDING/SETTLED) arrive in Phase 7. */
public enum TransferStatus {
    COMPLETED,
    FAILED,
    REVERSED
}
