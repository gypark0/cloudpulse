package com.gayoung.cloudpulse.controller;

import com.gayoung.cloudpulse.dto.CpuMetricResponse;
import com.gayoung.cloudpulse.service.CloudWatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cloudwatch")
public class CloudWatchController {

    private final CloudWatchService cloudWatchService;

    public CloudWatchController(CloudWatchService cloudWatchService) {
        this.cloudWatchService = cloudWatchService;
    }

    @GetMapping("/cpu")
    public List<CpuMetricResponse> getCpuMetrics() {
        return cloudWatchService.getCpuMetrics();
    }
}