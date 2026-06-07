package com.harrified.finCore.iam.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password,
        // Identifies which tenant this login belongs to — same email can exist in multiple tenants.
        @NotBlank String tenantSlug
) {}
