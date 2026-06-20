/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.service.impl;

import com.harrified.finCore.iam.domain.entity.User;
import com.harrified.finCore.iam.dto.UpdateUserRequest;
import com.harrified.finCore.iam.dto.UserResponse;
import com.harrified.finCore.iam.exception.NotFoundException;
import com.harrified.finCore.iam.repository.UserRepository;
import com.harrified.finCore.iam.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getById(UUID userId) {
        return UserResponse.from(findOrThrow(userId));
    }

    @Override
    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        User user = findOrThrow(userId);

        if (request.firstName() != null)  user.setFirstName(request.firstName());
        if (request.lastName() != null)   user.setLastName(request.lastName());
        if (request.phoneNumber() != null) user.setPhoneNumber(request.phoneNumber());

        if (request.address() != null) {
            // Merge address — a null sub-field within address clears that column.
            user.setAddress(request.address().toEmbeddable());
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Override
    public Page<UserResponse> listByTenant(UUID tenantId, Pageable pageable) {
        return userRepository.findAllByTenantId(tenantId, pageable)
                .map(UserResponse::from);
    }

    private User findOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }
}
