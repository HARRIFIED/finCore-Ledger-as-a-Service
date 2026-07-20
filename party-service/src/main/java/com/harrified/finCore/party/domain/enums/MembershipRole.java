/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.party.domain.enums;

/**
 * What a login (subject) may do on behalf of a party (PTY-4). Retail is typically just
 * {@code OWNER}; corporate uses maker/checker ({@code INITIATOR} proposes, {@code APPROVER}
 * authorizes) plus {@code VIEWER}/{@code SIGNATORY}/{@code ADMIN}.
 */
public enum MembershipRole {
    OWNER,
    ADMIN,
    INITIATOR,
    APPROVER,
    VIEWER,
    SIGNATORY
}
