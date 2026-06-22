package com.gayoung.cloudpulse.controller;

import com.gayoung.cloudpulse.dto.RecommendationResponse;
import com.gayoung.cloudpulse.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/api/recommendations")
    public List<RecommendationResponse> getRecommendations() {
        return recommendationService.getRecommendations();
    }
}
