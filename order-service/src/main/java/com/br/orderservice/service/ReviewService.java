package com.br.orderservice.service;

import com.br.orderservice.config.RabbitConfig;
import com.br.orderservice.controller.ReviewController;
import com.br.orderservice.model.Dish;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReviewService {

    private final RabbitTemplate rabbitTemplate;
    private final DishService dishService;

    public ReviewService(RabbitTemplate rabbitTemplate, DishService dishService) {
        this.rabbitTemplate = rabbitTemplate;
        this.dishService = dishService;
    }

    public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {}

    public void submitReview(Long dishId, int rating, String comment) {
        Dish dish = dishService.findById(dishId);

        var message = new ReviewMessage(dish.getId(), dish.getName(), rating, comment);
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.REVIEWS_ROUTING_KEY,
                message);

        log.info("Review published - dishId: {}, rating: {}", dishId, rating);
    }
}
