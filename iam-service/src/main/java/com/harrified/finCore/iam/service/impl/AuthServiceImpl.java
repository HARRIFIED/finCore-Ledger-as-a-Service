/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.service.impl;

import com.harrified.finCore.iam.domain.entity.KycProfile;
import com.harrified.finCore.iam.domain.entity.RefreshToken;
import com.harrified.finCore.iam.domain.entity.Tenant;
import com.harrified.finCore.iam.domain.entity.User;
import com.harrified.finCore.iam.domain.enums.UserRole;
import com.harrified.finCore.iam.domain.enums.UserStatus;
import com.harrified.finCore.iam.dto.LoginRequest;
import com.harrified.finCore.iam.dto.RefreshTokenRequest;
import com.harrified.finCore.iam.dto.RegisterRequest;
import com.harrified.finCore.iam.dto.TokenResponse;
import com.harrified.finCore.iam.exception.ConflictException;
import com.harrified.finCore.iam.exception.ForbiddenException;
import com.harrified.finCore.iam.exception.UnauthorizedException;
import com.harrified.finCore.iam.repository.KycProfileRepository;
import com.harrified.finCore.iam.repository.RefreshTokenRepository;
import com.harrified.finCore.iam.repository.TenantRepository;
import com.harrified.finCore.iam.repository.UserRepository;
import com.harrified.finCore.iam.security.JwtService;
import com.harrified.finCore.iam.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final KycProfileRepository kycProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.refresh-expiry-seconds:604800}")
    private long refreshExpirySeconds;

    public AuthServiceImpl(TenantRepository tenantRepository,
                           UserRepository userRepository,
                           KycProfileRepository kycProfileRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.kycProfileRepository = kycProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public TokenResponse register(RegisterRequest request) {
        if (tenantRepository.existsBySlug(request.tenantSlug())) {
            throw new ConflictException("Tenant slug already taken: " + request.tenantSlug());
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(request.tenantName())
                .slug(request.tenantSlug())
                .build());

        User user = userRepository.save(User.builder()
                .tenantId(tenant.getId())
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phoneNumber(request.phoneNumber())
                .role(UserRole.TENANT_ADMIN)
                .build());

        kycProfileRepository.save(KycProfile.builder()
                .userId(user.getId())
                .tenantId(tenant.getId())
                .build());

        return issueTokens(user);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        // Resolve tenant first — but surface the same "Invalid credentials" error
        // regardless of whether the tenant, user, or password is wrong (no enumeration).
        Tenant tenant = tenantRepository.findBySlug(request.tenantSlug())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        User user = userRepository.findByEmailAndTenantId(request.email().toLowerCase(), tenant.getId())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ForbiddenException("Account is suspended");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ForbiddenException("Account is inactive");
        }

        return issueTokens(user);
    }

    @Override
    public TokenResponse refresh(RefreshTokenRequest request) {
        String hash = jwtService.hashToken(request.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (stored.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        // Rotate: revoke the old token before issuing a new pair.
        refreshTokenRepository.revokeByTokenHash(hash);

        return issueTokens(user);
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        String hash = jwtService.hashToken(request.refreshToken());
        refreshTokenRepository.revokeByTokenHash(hash);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefresh = jwtService.generateRefreshToken();

        refreshTokenRepository.save(RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(jwtService.hashToken(rawRefresh))
                .expiresAt(Instant.now().plusSeconds(refreshExpirySeconds))
                .build());

        return TokenResponse.of(accessToken, rawRefresh, jwtService.getExpirySeconds());
    }
}
