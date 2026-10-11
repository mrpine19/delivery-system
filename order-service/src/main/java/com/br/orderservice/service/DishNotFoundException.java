package com.br.orderservice.service;

public class DishNotFoundException extends RuntimeException {

    public DishNotFoundException(Long id) {
        super("Dish not found");
    }
}