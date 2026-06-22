package com.gayoung.cloudpulse.dto;

public record RecommendationResponse(
        String category,
        String targetName,
        String targetId,
        String severity,
        String title,
        String currentStatus,
        String recommendation
) {
}
