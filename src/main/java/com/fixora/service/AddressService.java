package com.fixora.service;

import com.fixora.dto.request.AddressRequestDTO;
import com.fixora.dto.response.AddressResponseDTO;

import java.util.List;

public interface AddressService {

    List<AddressResponseDTO> listMyAddresses(Long customerId);

    AddressResponseDTO create(Long customerId, AddressRequestDTO request);

    AddressResponseDTO update(Long customerId, Long addressId, AddressRequestDTO request);

    void delete(Long customerId, Long addressId);

    AddressResponseDTO setDefault(Long customerId, Long addressId);
}
