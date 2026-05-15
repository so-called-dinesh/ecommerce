package com.dinesh.ecommerce.controllers;

import com.dinesh.ecommerce.model.OrderItem;
import com.dinesh.ecommerce.model.dto.OrderRequest;
import com.dinesh.ecommerce.model.dto.OrderResponse;
import com.dinesh.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.hibernate.type.OrderedSetType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/order")
@CrossOrigin
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest orderRequest){
        OrderResponse orderResponse = orderService.placeOrder(orderRequest);
        return new ResponseEntity<>(orderResponse, HttpStatus.CREATED);
    }

    @GetMapping("/getAllOrders")
    public ResponseEntity<List<OrderResponse>> getAllOrders(){
        List<OrderResponse> responses = orderService.getAllOrdersResponses();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }
}
