/**
 * Copyright (c) 2026-present Harrified tech and contributors
 * SPDX-License-Identifier: AGPL-3.0-only
 * See the LICENSE file for details.
 */

package com.harrified.finCore.iam.dto;

import com.harrified.finCore.iam.domain.embeddable.Address;

public record AddressDto(
        String street,
        String city,
        String state,
        String country,
        String postalCode
) {
    public static AddressDto from(Address address) {
        if (address == null) return null;
        return new AddressDto(
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPostalCode()
        );
    }

    public Address toEmbeddable() {
        return Address.builder()
                .street(street)
                .city(city)
                .state(state)
                .country(country)
                .postalCode(postalCode)
                .build();
    }
}
