package com.fixora.dto.response;

import com.fixora.enums.AddressType;

public record AddressResponseDTO(
        Long id,
        String label,
        String line1,
        String line2,
        String city,
        String area,
        String pincode,
        AddressType addressType,
        Boolean defaultAddress,
        String fullAddress
) {
}
