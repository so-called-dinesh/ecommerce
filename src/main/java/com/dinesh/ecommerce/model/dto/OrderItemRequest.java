package com.dinesh.ecommerce.model.dto;

public record OrderItemRequest(
        long productId,
        int quantity
) {}
