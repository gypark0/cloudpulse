package com.gayoung.cloudpulse.dto;

public record Ec2InstanceResponse(
        String instanceId,
        String state,
        String instanceType,
        String publicIp
) {
}
