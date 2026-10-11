package com.br.orderservice.exception;

public class PaymentUnavailableException extends RuntimeException {

    public PaymentUnavailableException() {
        super("Payment unavailable");
    }
}