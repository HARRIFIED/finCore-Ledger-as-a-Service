/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.service;

import com.harrified.finCore.iam.dto.LoginRequest;
import com.harrified.finCore.iam.dto.RefreshTokenRequest;
import com.harrified.finCore.iam.dto.RegisterRequest;
import com.harrified.finCore.iam.dto.TokenResponse;

public interface AuthService {

    TokenResponse register(RegisterRequest request);

    TokenResponse login(LoginRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);
}
