/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.platform.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setGetClear() {
        assertNull(TenantContext.get());
        TenantContext.set("tenant-1");
        assertEquals("tenant-1", TenantContext.get());
        TenantContext.clear();
        assertNull(TenantContext.get());
    }

    @Test
    void requireThrowsWhenAbsent() {
        assertThrows(IllegalStateException.class, TenantContext::require);
    }

    @Test
    void requireReturnsWhenPresent() {
        TenantContext.set("tenant-2");
        assertEquals("tenant-2", TenantContext.require());
    }
}
