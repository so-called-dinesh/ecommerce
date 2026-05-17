package com.dinesh.ecommerce.service;

import com.dinesh.ecommerce.model.Order;
import com.dinesh.ecommerce.model.OrderItem;
import com.dinesh.ecommerce.model.Product;
import com.dinesh.ecommerce.model.dto.OrderItemRequest;
import com.dinesh.ecommerce.model.dto.OrderItemResponse;
import com.dinesh.ecommerce.model.dto.OrderRequest;
import com.dinesh.ecommerce.model.dto.OrderResponse;
import com.dinesh.ecommerce.repo.OrderRepo;
import com.dinesh.ecommerce.repo.ProductRepo;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private OrderRepo orderRepo;

    @Transactional
    public OrderResponse placeOrder(OrderRequest request) {
        Order order = new Order();

        String orderId = "ORD" + UUID.randomUUID().toString().substring(0,8).toUpperCase();
        order.setOrderId(orderId);
        order.setCustomerName(request.customerName());
        order.setEmail(request.email());
        order.setStatus("PLACED");
        order.setOrderDate(LocalDate.now());
        List<OrderItem> orderItems = new ArrayList<>();

        for(OrderItemRequest itemRequest : request.items()){

            Product product = productRepo.findById(itemRequest.productId())
                    .orElseThrow(() -> new RuntimeException("product not found"));

            if(!product.getProductAvailable()){
                throw new RuntimeException("Product is not available :" + product.getName());
            }
            if(product.getStockQuantity() < itemRequest.quantity()){
                throw new RuntimeException("Insufficient Stock for the product " + product.getName() + ". Available: " + product.getStockQuantity()
                        + ". Requested: " + itemRequest.quantity());
            }
            product.setStockQuantity(product.getStockQuantity() - itemRequest.quantity());
            productRepo.save(product);

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.quantity())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.quantity())))
                    .order(order)
                    .build();

            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);
        Order savedOrder = orderRepo.save(order);

        List<OrderItemResponse> itemResponses = new ArrayList<>();
        for(OrderItem item : order.getOrderItems()){
            OrderItemResponse orderItemResponse = new OrderItemResponse(
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getTotalPrice()
            );
            itemResponses.add(orderItemResponse);
        }

        OrderResponse orderResponse = new OrderResponse(
                savedOrder.getOrderId(),
                savedOrder.getCustomerName(),
                savedOrder.getEmail(),
                savedOrder.getStatus(),
                savedOrder.getOrderDate(),
                itemResponses
        );

        return orderResponse;
    }

    public List<OrderResponse> getAllOrdersResponses() {
        List<Order> orders = orderRepo.findAll();
        List<OrderResponse> orderResponses = new ArrayList<>();

        for(Order order : orders){

            List<OrderItemResponse> itemResponses = new ArrayList<>();

            for(OrderItem item : order.getOrderItems()){
                OrderItemResponse orderItemResponse = new OrderItemResponse(
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getTotalPrice()
                );
                itemResponses.add(orderItemResponse);
            }
                OrderResponse orderResponse = new OrderResponse(
                        order.getOrderId(),
                        order.getCustomerName(),
                        order.getEmail(),
                        order.getStatus(),
                        order.getOrderDate(),
                        itemResponses
                );
                orderResponses.add(orderResponse);
        }

        return orderResponses;
    }
}
