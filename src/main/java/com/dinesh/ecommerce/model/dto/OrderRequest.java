package com.dinesh.ecommerce.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "customer name required")
        String customerName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email,

        @NotEmpty(message = "order must have atleast one item")
        @Valid
        List<OrderItemRequest> items
) {}

