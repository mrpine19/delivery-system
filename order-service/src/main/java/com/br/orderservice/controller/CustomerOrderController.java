package com.br.orderservice.controller;

import com.br.orderservice.model.CustomerOrder;
import com.br.orderservice.service.CustomerOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class CustomerOrderController {
    private final CustomerOrderService customerOrderService;

    public CustomerOrderController(CustomerOrderService customerOrderService) {
        this.customerOrderService = customerOrderService;
    }

    public record CreateOrderRequest(
            @NotNull Long dishId,
            @Min(1) int quantity
    ) {}

    @PostMapping
    public ResponseEntity<CustomerOrder> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(customerOrderService.createOrder(request.dishId(), request.quantity()));
    }

    @GetMapping("/{id}")
    public CustomerOrder getOrder(@PathVariable Long id) {
        return customerOrderService.getOrder(id);
    }
}
