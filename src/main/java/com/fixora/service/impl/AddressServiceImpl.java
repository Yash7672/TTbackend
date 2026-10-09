package com.fixora.service.impl;

import com.fixora.dto.request.AddressRequestDTO;
import com.fixora.dto.response.AddressResponseDTO;
import com.fixora.entity.Address;
import com.fixora.entity.User;
import com.fixora.enums.AddressType;
import com.fixora.exception.ResourceNotFoundException;
import com.fixora.mapper.AddressMapper;
import com.fixora.repository.AddressRepository;
import com.fixora.repository.UserRepository;
import com.fixora.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponseDTO> listMyAddresses(Long customerId) {
        return addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customerId)
                .stream().map(addressMapper::toDto).toList();
    }

    @Override
    @Transactional
    public AddressResponseDTO create(Long customerId, AddressRequestDTO request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", customerId));

        boolean firstAddress = addressRepository.countByCustomerId(customerId) == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(request.defaultAddress());

        if (makeDefault) {
            clearExistingDefault(customerId);
        }

        Address address = Address.builder()
                .customer(customer)
                .label(request.label())
                .line1(request.line1().trim())
                .line2(request.line2())
                .city(request.city().trim())
                .area(request.area())
                .pincode(request.pincode())
                .addressType(request.addressType() == null ? AddressType.HOME : request.addressType())
                .defaultAddress(makeDefault)
                .build();

        return addressMapper.toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponseDTO update(Long customerId, Long addressId, AddressRequestDTO request) {
        Address address = loadOwned(customerId, addressId);

        address.setLabel(request.label());
        address.setLine1(request.line1().trim());
        address.setLine2(request.line2());
        address.setCity(request.city().trim());
        address.setArea(request.area());
        address.setPincode(request.pincode());
        if (request.addressType() != null) {
            address.setAddressType(request.addressType());
        }
        if (Boolean.TRUE.equals(request.defaultAddress())) {
            clearExistingDefault(customerId);
            address.setDefaultAddress(true);
        }

        return addressMapper.toDto(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void delete(Long customerId, Long addressId) {
        Address address = loadOwned(customerId, addressId);
        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());
        addressRepository.delete(address);

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customerId);
            if (!remaining.isEmpty()) {
                Address promoted = remaining.get(0);
                promoted.setDefaultAddress(true);
                addressRepository.save(promoted);
            }
        }
    }

    @Override
    @Transactional
    public AddressResponseDTO setDefault(Long customerId, Long addressId) {
        Address address = loadOwned(customerId, addressId);
        clearExistingDefault(customerId);
        address.setDefaultAddress(true);
        return addressMapper.toDto(addressRepository.save(address));
    }

    /** Ownership is always re-checked against the database. */
    private Address loadOwned(Long customerId, Long addressId) {
        return addressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address " + addressId + " was not found for this account."));
    }

    private void clearExistingDefault(Long customerId) {
        for (Address existing : addressRepository.findByCustomerIdOrderByDefaultAddressDescIdDesc(customerId)) {
            if (Boolean.TRUE.equals(existing.getDefaultAddress())) {
                existing.setDefaultAddress(false);
                addressRepository.save(existing);
            }
        }
    }
}
