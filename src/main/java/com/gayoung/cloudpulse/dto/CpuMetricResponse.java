package com.gayoung.cloudpulse.dto;

public record CpuMetricResponse(
        String instanceId,
        String instanceName,
        String region,
        String state,
        double averageCpu,
        double maxCpu,
        String status
) {
}
