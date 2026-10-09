package com.fixora.controller;

import com.fixora.dto.request.AddressRequestDTO;
import com.fixora.dto.response.AddressResponseDTO;
import com.fixora.service.AddressService;
import com.fixora.validation.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public List<AddressResponseDTO> list(@CurrentUserId Long customerId) {
        return addressService.listMyAddresses(customerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponseDTO create(@CurrentUserId Long customerId,
                                     @Valid @RequestBody AddressRequestDTO request) {
        return addressService.create(customerId, request);
    }

    @PutMapping("/{id}")
    public AddressResponseDTO update(@CurrentUserId Long customerId,
                                     @PathVariable Long id,
                                     @Valid @RequestBody AddressRequestDTO request) {
        return addressService.update(customerId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUserId Long customerId, @PathVariable Long id) {
        addressService.delete(customerId, id);
    }

    @PutMapping("/{id}/default")
    public AddressResponseDTO setDefault(@CurrentUserId Long customerId, @PathVariable Long id) {
        return addressService.setDefault(customerId, id);
    }
}
