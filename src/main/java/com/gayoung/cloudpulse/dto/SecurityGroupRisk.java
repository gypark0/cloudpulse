package com.gayoung.cloudpulse.dto;

public record SecurityGroupRisk(
        String type,
        String message,
        String severity
) {
}