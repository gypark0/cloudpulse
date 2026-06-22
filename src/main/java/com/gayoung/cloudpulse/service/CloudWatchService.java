package com.gayoung.cloudpulse.service;

import com.gayoung.cloudpulse.dto.CpuMetricResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CloudWatchService {

    public List<CpuMetricResponse> getCpuMetrics() {
        List<CpuMetricResponse> result = new ArrayList<>();

        try (Ec2Client defaultEc2Client = Ec2Client.create()) {
            DescribeRegionsResponse regionsResponse = defaultEc2Client.describeRegions();

            for (software.amazon.awssdk.services.ec2.model.Region ec2Region : regionsResponse.regions()) {
                String regionName = ec2Region.regionName();

                try (
                        Ec2Client ec2Client = Ec2Client.builder()
                                .region(Region.of(regionName))
                                .build();

                        CloudWatchClient cloudWatchClient = CloudWatchClient.builder()
                                .region(Region.of(regionName))
                                .build()
                ) {
                    DescribeInstancesResponse instancesResponse = ec2Client.describeInstances();

                    for (Reservation reservation : instancesResponse.reservations()) {
                        for (Instance instance : reservation.instances()) {
                            String instanceId = instance.instanceId();
                            String instanceName = getInstanceName(instance);
                            String state = instance.state().nameAsString();

                            CpuMetricResponse cpuMetric = getCpuMetric(
                                    cloudWatchClient,
                                    instanceId,
                                    instanceName,
                                    regionName,
                                    state
                            );

                            result.add(cpuMetric);
                        }
                    }
                }
            }
        }

        return result;
    }

    private CpuMetricResponse getCpuMetric(
            CloudWatchClient cloudWatchClient,
            String instanceId,
            String instanceName,
            String region,
            String state
    ) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(1, ChronoUnit.HOURS);

        Dimension instanceDimension = Dimension.builder()
                .name("InstanceId")
                .value(instanceId)
                .build();

        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .namespace("AWS/EC2")
                .metricName("CPUUtilization")
                .dimensions(instanceDimension)
                .statistics(Statistic.AVERAGE, Statistic.MAXIMUM)
                .unit(StandardUnit.PERCENT)
                .startTime(startTime)
                .endTime(endTime)
                .period(300)
                .build();

        GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

        double averageCpu = response.datapoints().stream()
                .map(Datapoint::average)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double maxCpu = response.datapoints().stream()
                .map(Datapoint::maximum)
                .filter(value -> value != null)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        String status = judgeStatus(averageCpu, maxCpu);

        return new CpuMetricResponse(
                instanceId,
                instanceName,
                region,
                state,
                averageCpu,
                maxCpu,
                status
        );
    }

    private String judgeStatus(double averageCpu, double maxCpu) {
        if (averageCpu >= 80) {
            return "과부하 위험";
        }

        if (averageCpu >= 60 || maxCpu >= 90) {
            return "주의";
        }

        if (averageCpu < 5) {
            return "저사용";
        }

        return "정상";
    }

    private String getInstanceName(Instance instance) {
        for (Tag tag : instance.tags()) {
            if ("Name".equals(tag.key())) {
                return tag.value();
            }
        }

        return "-";
    }
}