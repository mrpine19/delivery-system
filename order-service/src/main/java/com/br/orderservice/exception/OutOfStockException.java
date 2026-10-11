package com.br.orderservice.exception;

public class OutOfStockException extends RuntimeException {

    public OutOfStockException() {
        super("Dish out of stock");
    }
}