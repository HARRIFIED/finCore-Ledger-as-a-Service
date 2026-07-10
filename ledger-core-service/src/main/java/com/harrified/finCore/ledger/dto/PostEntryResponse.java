/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.ledger.dto;

import java.util.UUID;

public record PostEntryResponse(UUID entryId, String status) {
}
