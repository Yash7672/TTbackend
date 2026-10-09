package com.fixora.mapper;

import com.fixora.dto.response.AddressResponseDTO;
import com.fixora.entity.Address;
import org.springframework.stereotype.Component;

import java.util.StringJoiner;

@Component
public class AddressMapper {

    public AddressResponseDTO toDto(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponseDTO(
                address.getId(),
                address.getLabel(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getArea(),
                address.getPincode(),
                address.getAddressType(),
                address.getDefaultAddress(),
                summarise(address)
        );
    }

    public String summarise(Address address) {
        if (address == null) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ");
        joiner.add(address.getLine1());
        if (address.getLine2() != null && !address.getLine2().isBlank()) {
            joiner.add(address.getLine2());
        }
        if (address.getArea() != null && !address.getArea().isBlank()) {
            joiner.add(address.getArea());
        }
        joiner.add(address.getCity());
        if (address.getPincode() != null && !address.getPincode().isBlank()) {
            joiner.add(address.getPincode());
        }
        return joiner.toString();
    }
}
