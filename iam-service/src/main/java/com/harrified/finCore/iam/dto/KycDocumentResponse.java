/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.dto;

import com.harrified.finCore.iam.domain.entity.KycDocument;

import java.time.Instant;
import java.util.UUID;

public record KycDocumentResponse(
        UUID id,
        UUID userId,
        String type,
        String fileName,
        String mimeType,
        String status,
        String rejectionReason,
        // Presigned download URL — populated by the service layer, not stored in DB.
        String downloadUrl,
        Instant reviewedAt,
        Instant createdAt
) {
    public static KycDocumentResponse from(KycDocument doc, String downloadUrl) {
        return new KycDocumentResponse(
                doc.getId(),
                doc.getUserId(),
                doc.getType().name(),
                doc.getFileName(),
                doc.getMimeType(),
                doc.getStatus().name(),
                doc.getRejectionReason(),
                downloadUrl,
                doc.getReviewedAt(),
                doc.getCreatedAt()
        );
    }
}
