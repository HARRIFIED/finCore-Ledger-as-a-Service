#!/bin/bash
# Copyright (c) 2026-present Harrified tech and contributors
# SPDX-License-Identifier: AGPL-3.0-only
# See the LICENSE file for details.

set -euo pipefail

echo "[localstack] bootstrapping AWS resources..."

# ── S3 ───────────────────────────────────────────────────────────────────────

# KYC document storage — iam-service stores file keys here, generates presigned URLs on demand
awslocal s3 mb s3://fincore-kyc-documents --region us-east-1

# Versioning provides a lightweight audit trail for uploaded documents
awslocal s3api put-bucket-versioning \
  --bucket fincore-kyc-documents \
  --versioning-configuration Status=Enabled

echo "[localstack] s3://fincore-kyc-documents created"

# ── Secrets Manager ──────────────────────────────────────────────────────────

# Placeholder secrets — populated in CI/staging with real values
awslocal secretsmanager create-secret \
  --name fincore/iam/jwt-private-key \
  --secret-string "REPLACE_ME_IN_STAGING" \
  --region us-east-1

echo "[localstack] secrets created"
echo "[localstack] bootstrap complete"
