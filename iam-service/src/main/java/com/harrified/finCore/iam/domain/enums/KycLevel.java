package com.harrified.finCore.iam.domain.enums;

/**
 * Tiered KYC levels aligned with CBN and common banking-regulator requirements.
 * Each tier unlocks higher transaction limits set by the tenant.
 *
 * NONE    — registration only, no verification
 * BASIC   — phone/email verified
 * TIER_1  — BVN or NIN verified (soft KYC)
 * TIER_2  — government-issued ID document approved
 * FULL    — all documents approved, address verified (full KYC)
 */
public enum KycLevel {
    NONE,
    BASIC,
    TIER_1,
    TIER_2,
    FULL
}
