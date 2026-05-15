package com.dinesh.ecommerce.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PostMapping;

public record OrderItemRequest(

        @Positive(message = "product id must be positive")
        long productId,
        @Min(value =1, message = "Quantity must be atleast one")
        int quantity
) {}
