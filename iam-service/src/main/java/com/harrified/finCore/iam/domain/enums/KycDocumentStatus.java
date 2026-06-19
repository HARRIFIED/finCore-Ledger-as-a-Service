/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.domain.enums;

public enum KycDocumentStatus {
    PENDING,        // uploaded, awaiting review
    UNDER_REVIEW,   // assigned to a reviewer
    APPROVED,
    REJECTED        // rejection reason stored on the document
}
