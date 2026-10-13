package com.br.orderservice.service;

import com.br.orderservice.model.Dish;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Locale PT_BR = Locale.of("pt", "BR");

    private final ChatClient chatClient;
    private final DishService dishService;

    private static final String SYSTEM_PROMPT = """
            You are a friendly attendant of a restaurant that sells through a delivery app.

            Rules:
            - Always answer in Brazilian Portuguese, in at most 3 short sentences.
            - Use ONLY the menu below. Never invent dishes, ingredients or prices.
            - If a dish is marked UNAVAILABLE, say it is sold out and suggest an available alternative.
            - If the question is not about the restaurant, its menu, prices or orders,
              politely refuse and offer help with the menu.

            MENU:
            {menu}
            """;

    public ChatService(ChatClient.Builder builder, DishService dishService) {
        this.chatClient = builder.defaultSystem(SYSTEM_PROMPT).build();
        this.dishService = dishService;
    }


    public String getAnswer(String question) {
        String menu = buildMenu();

        return chatClient.prompt()
                .system(s -> s.param("menu", menu))
                .user(question)
                .call()
                .content();
    }

    private String buildMenu() {
        return dishService.findAllDishes().stream()
                .map(this::formatDish)
                .collect(Collectors.joining("\n"));
    }

    private String formatDish(Dish dish) {
        String availability = dish.getStock() > 0
                ? "AVAILABLE (" + dish.getStock() + " left)"
                : "UNAVAILABLE (sold out)";

        return String.format(PT_BR, "- %s | %s | R$ %.2f | %s",
                dish.getName(), dish.getDescription(), dish.getPrice(), availability);
    }
}
