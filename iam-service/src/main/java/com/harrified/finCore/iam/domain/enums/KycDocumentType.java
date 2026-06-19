/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.domain.enums;

public enum KycDocumentType {
    // Identity documents
    PASSPORT,
    NATIONAL_ID,
    DRIVERS_LICENSE,
    VOTERS_CARD,

    // Biometric / number-based
    BVN_SLIP,
    NIN_SLIP,

    // Address verification
    PROOF_OF_ADDRESS,
    UTILITY_BILL,
    BANK_STATEMENT,

    // Selfie / liveness
    SELFIE,

    // Business documents (for non-individual / corporate tenants)
    BUSINESS_REGISTRATION,
    TAX_IDENTIFICATION,

    // Catch-all for tenant-specific document types not yet in this enum.
    // The KycDocument.metadata field carries the custom type label in this case.
    OTHER
}
