package com.br.reviewservice.repository;

import com.br.reviewservice.model.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {
}
