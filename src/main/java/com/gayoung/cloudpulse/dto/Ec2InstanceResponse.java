package com.gayoung.cloudpulse.dto;

import java.util.List;

public record Ec2InstanceResponse(
        String instanceId,
        String instanceName,
        String state,
        String instanceType,
        boolean hasPublicIp,
        String publicIp,
        List<String> securityGroups,
        String region,
        String availabilityZone,
        String environment
) {
}
