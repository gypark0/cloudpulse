package com.gayoung.cloudpulse.dto;

import java.util.List;

public record DashboardResponse(
        List<Ec2InstanceResponse> instances,
        List<CpuMetricResponse> cpuMetrics,
        List<SecurityGroupCheckResponse> securityGroups,
        List<RecommendationResponse> recommendations
) {
}