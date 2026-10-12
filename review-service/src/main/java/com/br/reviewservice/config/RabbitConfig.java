package com.br.reviewservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String REVIEW_QUEUE = "reviews.queue";
    public static final String EXCHANGE = "delivery.exchange";
    public static final String REVIEWS_ROUTING_KEY = "reviews.new";

    // queue
    @Bean
    Queue queue(){
        return new Queue(REVIEW_QUEUE, true);
    }

    // exchange
    @Bean
    TopicExchange exchange(){
        return new TopicExchange(EXCHANGE);
    }

    // binding
    @Bean
    Binding binding(){
        return BindingBuilder.bind(queue()).to(exchange()).with(REVIEWS_ROUTING_KEY);
    }

    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
