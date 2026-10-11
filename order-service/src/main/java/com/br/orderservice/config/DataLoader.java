package com.br.orderservice.config;

import com.br.orderservice.model.Dish;
import com.br.orderservice.repository.DishRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner loadDishes(DishRepository dishRepository) {
        return args -> {
            if (dishRepository.count() == 0) {
                dishRepository.saveAll(List.of(
                        buildDish("X-Burger", "Hambúrguer artesanal com queijo e molho da casa", new BigDecimal("39.90"), 12),
                        buildDish("Coca-Cola", "Refrigerante gelada 600ml", new BigDecimal("8.50"), 25),
                        buildDish("Salada Verde", "Mix de folhas verdes, tomate e molho de ervas; prato vegetariano/vegano", new BigDecimal("24.00"), 18),
                        buildDish("Promo do Dia", "Combo especial com batata e bebida, prato da promoção", new BigDecimal("29.90"), 10),
                        buildDish("Frango à Parmegiana", "Frango empanado com molho e queijo", new BigDecimal("42.00"), 9)
                ));
            }
        };
    }

    private Dish buildDish(String name, String description, BigDecimal price, int stock) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(description);
        dish.setPrice(price);
        dish.setStock(stock);
        return dish;
    }
}