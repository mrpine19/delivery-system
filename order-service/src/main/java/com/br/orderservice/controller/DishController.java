package com.br.orderservice.controller;

import com.br.orderservice.dto.response.DishResponse;
import com.br.orderservice.service.DishService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dishes")
public class DishController {

    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping
    public List<DishResponse> getAllDishes() {
        return dishService.findAll().stream()
                .map(DishResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public DishResponse getDishById(@PathVariable Long id) {
        return DishResponse.from(dishService.findById(id));
    }
}