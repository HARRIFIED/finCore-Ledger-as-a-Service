package com.harrified.finCore.iam.dto;

import com.harrified.finCore.iam.domain.entity.KycProfile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record KycProfileResponse(
        UUID id,
        UUID userId,
        String kycLevel,
        LocalDate dateOfBirth,
        boolean hasBvn,        // never exposing the actual BVN value in a response
        boolean hasNin,
        Instant verifiedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static KycProfileResponse from(KycProfile profile) {
        return new KycProfileResponse(
                profile.getId(),
                profile.getUserId(),
                profile.getKycLevel().name(),
                profile.getDateOfBirth(),
                profile.getBvn() != null,
                profile.getNin() != null,
                profile.getVerifiedAt(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
