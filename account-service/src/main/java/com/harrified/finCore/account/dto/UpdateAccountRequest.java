/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.account.dto;

import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

public record UpdateAccountRequest(
    @Size(max = 255) String accountName,
    String description,
    Map<String, Object> metadata,
    List<String> tags
) {}