package com.br.orderservice.service;

import com.br.orderservice.exception.DishNotFoundException;
import com.br.orderservice.model.Dish;
import com.br.orderservice.repository.DishRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishService {

    private final DishRepository dishRepository;

    public DishService(DishRepository dishRepository) {
        this.dishRepository = dishRepository;
    }

    public List<Dish> findAll() {
        return dishRepository.findAll();
    }

    public Dish findById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new DishNotFoundException(id));
    }
}