package com.br.reviewservice.service;

import com.br.reviewservice.config.RabbitConfig;
import com.br.reviewservice.model.ReviewSummary;
import com.br.reviewservice.repository.ReviewSummaryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
        summary.setRatingSum(summary.getRatingSum() + acc.ratingSum());
        summary.setCount(summary.getCount() + acc.count());
        reviewSummaryRepository.save(summary);

        log.info("Processed review summary for dishId: {}, total reviews: {}", dishId, acc.count());
    }
}