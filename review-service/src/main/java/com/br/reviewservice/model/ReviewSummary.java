package com.br.reviewservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {

    @Id
    private Long dishId;
    private String dishName;
    private Integer ratingSum;
    private Integer count;
}
