package com.br.reviewservice.service;

import com.br.reviewservice.config.RabbitConfig;
import com.br.reviewservice.controller.ReviewSummaryController;
import com.br.reviewservice.model.ReviewSummary;
import com.br.reviewservice.repository.ReviewSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ReviewSummaryService {

    public record ReviewMessage(Long dishId, String dishName, int rating, String comment) {}

    record ReviewAccumulator(String dishName, int ratingSum, int count) {}

    private final ConcurrentHashMap<Long, ReviewAccumulator> buffer = new ConcurrentHashMap<>();
    private final ReviewSummaryRepository reviewSummaryRepository;

    public ReviewSummaryService(ReviewSummaryRepository reviewSummaryRepository) {
        this.reviewSummaryRepository = reviewSummaryRepository;
    }

    @RabbitListener(queues = RabbitConfig.REVIEW_QUEUE)
    public void onReview(ReviewMessage message) {
        buffer.merge(
                message.dishId(),
                new ReviewAccumulator(message.dishName(), message.rating(), 1),
                (current, incoming) -> new ReviewAccumulator(
                        current.dishName(),
                        current.ratingSum() + incoming.ratingSum(),
                        current.count() + incoming.count()));
        log.debug("Buffered review - dishId: {}, rating: {}", message.dishId(), message.rating());
    }

    @Scheduled(fixedRate = 5000)
    public void flush() {
        if (buffer.isEmpty())
            return;

        buffer.keySet().forEach(this::processReviewSummary);
    }

    public void processReviewSummary(Long dishId){
        ReviewAccumulator acc = buffer.remove(dishId);
        if (acc == null) return;

        ReviewSummary summary = reviewSummaryRepository.findById(dishId)
                .orElseGet(() -> new ReviewSummary(dishId, acc.dishName(), 0, 0));
        summary.setCount(summary.getCount() + acc.count());
        summary.setRatingSum(summary.getRatingSum() + acc.ratingSum());
        reviewSummaryRepository.save(summary);

        log.info("Processed review summary for dishId: {}, total reviews: {}", dishId, acc.count());
    }

    public List<ReviewSummaryController.RankingResponse> getRanking() {
        return reviewSummaryRepository.findAll().stream()
                .map(s -> new ReviewSummaryController.RankingResponse(
                        s.getDishId(),
                        s.getDishName(),
                        average(s.getRatingSum(), s.getCount()),
                        s.getCount()))
                .sorted(Comparator
                        .comparingDouble(ReviewSummaryController.RankingResponse::average).reversed()
                        .thenComparing(Comparator.comparingInt(ReviewSummaryController.RankingResponse::count).reversed()))
                .toList();
    }

    private double average(int ratingSum, int count) {
        return BigDecimal.valueOf(ratingSum)
                .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}