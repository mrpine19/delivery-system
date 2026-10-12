package com.br.orderservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
@Slf4j
public class PaymentClient {

    private static final String PAYMENT_URL = "http://PAYMENT-SERVICE/payments";

    public record PaymentRequest(BigDecimal amount) {}
    public record PaymentResponse(String status, int instance) {}

    private final RestTemplate restTemplate;

    public PaymentClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Retryable(
            includes = {HttpServerErrorException.class, ResourceAccessException.class},
            maxRetries = 4,
            delay = 200,
            multiplier = 2,
            jitter = 100,
            maxDelay = 2000
    )
    public PaymentResponse pay(BigDecimal amount) {
        log.info("Calling payment-service - amount: {}", amount);
        PaymentResponse response = restTemplate.postForObject(
                PAYMENT_URL, new PaymentRequest(amount), PaymentResponse.class);
        log.info("Payment APPROVED by instance {}", response.instance());
        return response;
    }
}
