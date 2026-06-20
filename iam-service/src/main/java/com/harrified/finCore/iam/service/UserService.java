/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.service;

import com.harrified.finCore.iam.dto.UpdateUserRequest;
import com.harrified.finCore.iam.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getById(UUID userId);

    UserResponse update(UUID userId, UpdateUserRequest request);

    Page<UserResponse> listByTenant(UUID tenantId, Pageable pageable);
}
