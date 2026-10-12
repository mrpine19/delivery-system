package com.br.reviewservice.controller;

import com.br.reviewservice.service.ReviewSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewSummaryController {

    private final ReviewSummaryService reviewSummaryService;

    public ReviewSummaryController(ReviewSummaryService reviewSummaryService) {
        this.reviewSummaryService = reviewSummaryService;
    }

    public record RankingResponse(Long dishId, String dishName, double average, int count) {}

    @GetMapping("/ranking")
    public List<RankingResponse> getRanking() {
        return reviewSummaryService.getRanking();
    }
}
