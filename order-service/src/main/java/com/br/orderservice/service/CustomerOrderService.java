package com.br.orderservice.service;

import com.br.orderservice.exception.DishNotFoundException;
import com.br.orderservice.exception.InvalidQuantityException;
import com.br.orderservice.exception.OrderNotFoundException;
import com.br.orderservice.exception.OutOfStockException;
import com.br.orderservice.model.CustomerOrder;
import com.br.orderservice.model.Dish;
import com.br.orderservice.model.OrderStatus;
import com.br.orderservice.repository.CustomerOrderRepository;
import com.br.orderservice.repository.DishRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CustomerOrderService {
    private final CustomerOrderRepository customerOrderRepository;
    private final DishRepository dishRepository;

    public CustomerOrderService(CustomerOrderRepository customerOrderRepository, DishRepository dishRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.dishRepository = dishRepository;
    }

    @Transactional
    public CustomerOrder createOrder(Long dishId, int quantity) {
        if (quantity < 1)
            throw new InvalidQuantityException("Quantity must be at least 1");

        Dish dish = dishRepository.findByIdForUpdate(dishId)
                .orElseThrow(() -> new DishNotFoundException(dishId));

        if (dish.getStock() < quantity)
            throw new OutOfStockException();


        BigDecimal total = dish.getPrice()
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        dish.setStock(dish.getStock() - quantity);
        return customerOrderRepository.save(new CustomerOrder(dishId, quantity, total, OrderStatus.CONFIRMED));
    }

    public CustomerOrder getOrder(Long id) {
        return customerOrderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}
