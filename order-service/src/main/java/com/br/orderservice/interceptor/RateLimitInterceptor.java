package com.br.orderservice.interceptor;

import com.br.orderservice.exception.TooManyRequestsException;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final Map<Method, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod))
            return true;

        RateLimited rateLimited = handlerMethod.getMethodAnnotation(RateLimited.class);
        if (rateLimited == null)
            return true;

        Bucket bucket = buckets.computeIfAbsent(handlerMethod.getMethod(),
                method -> newBucket(rateLimited.requestsPerSecond()));

        if (!bucket.tryConsume(1))
            throw new TooManyRequestsException("Too many requests");

        return true;
    }

    private Bucket newBucket(long requestsPerSecond) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(requestsPerSecond)
                        .refillIntervally(requestsPerSecond, Duration.ofSeconds(1)))
                .build();
    }
}
