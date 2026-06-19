package com.gayoung.cloudpulse.dto;

import java.util.List;

public record SecurityGroupCheckResponse(
        String region,
        String securityGroupId,
        String securityGroupName,
        String vpcId,
        boolean attachedToEc2,
        boolean publicIpExposed,
        boolean unusedCandidate,
        List<String> attachedInstanceIds,
        List<SecurityGroupRisk> risks
) {
}