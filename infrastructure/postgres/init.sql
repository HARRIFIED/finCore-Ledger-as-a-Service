-- Copyright (c) 2026-present Harrified tech and contributors
-- SPDX-License-Identifier: AGPL-3.0-only
-- See the LICENSE file for details.

-- Runs once at container startup as the finCore superuser.
-- Creates a dedicated database for each service so migrations are isolated.

CREATE DATABASE identity_db;
CREATE DATABASE account_db;
