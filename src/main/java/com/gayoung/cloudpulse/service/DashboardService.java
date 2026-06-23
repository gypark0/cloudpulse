package com.gayoung.cloudpulse.service;

import com.gayoung.cloudpulse.dto.CpuMetricResponse;
import com.gayoung.cloudpulse.dto.DashboardResponse;
import com.gayoung.cloudpulse.dto.Ec2InstanceResponse;
import com.gayoung.cloudpulse.dto.RecommendationResponse;
import com.gayoung.cloudpulse.dto.SecurityGroupCheckResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final Ec2Service ec2Service;
    private final CloudWatchService cloudWatchService;
    private final SecurityGroupService securityGroupService;
    private final RecommendationService recommendationService;

    public DashboardService(
            Ec2Service ec2Service,
            CloudWatchService cloudWatchService,
            SecurityGroupService securityGroupService,
            RecommendationService recommendationService
    ) {
        this.ec2Service = ec2Service;
        this.cloudWatchService = cloudWatchService;
        this.securityGroupService = securityGroupService;
        this.recommendationService = recommendationService;
    }

    public DashboardResponse getDashboard() {
        List<Ec2InstanceResponse> instances = ec2Service.getInstances();
        List<CpuMetricResponse> cpuMetrics = cloudWatchService.getCpuMetrics();
        List<SecurityGroupCheckResponse> securityGroups = securityGroupService.checkSecurityGroups();

        List<RecommendationResponse> recommendations =
                recommendationService.createRecommendations(
                        instances,
                        cpuMetrics,
                        securityGroups
                );

        return new DashboardResponse(
                instances,
                cpuMetrics,
                securityGroups,
                recommendations
        );
    }
}