package com.br.orderservice.controller;

import com.br.orderservice.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    public record ReviewRequest(
            @NotNull Long dishId,
            @Min(1) @Max(5) int rating,
            String comment
    ){}

    @PostMapping
    public ResponseEntity<Void> submitReview(@RequestBody @Valid ReviewRequest request) {
        reviewService.submitReview(request.dishId, request.rating(), request.comment());
        return ResponseEntity.accepted().build();
    }
}
