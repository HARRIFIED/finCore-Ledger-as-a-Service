package com.harrified.finCore.iam.domain.enums;

public enum KycDocumentStatus {
    PENDING,        // uploaded, awaiting review
    UNDER_REVIEW,   // assigned to a reviewer
    APPROVED,
    REJECTED        // rejection reason stored on the document
}
