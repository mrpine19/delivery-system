package com.br.paymentservice.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@RestController
@Slf4j
public class PaymentController {

    public record PaymentRequest(BigDecimal amount){}
    public record PaymentResponse(String status, int instance){}

    @Value("${server.port}")
    private int port;

    @PostMapping("/payments")
    public PaymentResponse processPayment(@RequestBody PaymentRequest request){
        var random = Math.random();

        if(random < 0.5){
            log.info("[instance {}] Payment APPROVED - amount: {}", port, request.amount());
            return new PaymentResponse("APPROVED", port);
        } else {
            log.warn("[instance {}] Payment FAILED (simulated) - amount: {}", port, request.amount());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Payment declined");
        }
    }
}