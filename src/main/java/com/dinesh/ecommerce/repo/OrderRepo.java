package com.dinesh.ecommerce.repo;

import com.dinesh.ecommerce.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepo extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderId(String orderId);
}
