package com.gayoung.cloudpulse.service;

import com.gayoung.cloudpulse.dto.CpuMetricResponse;
import com.gayoung.cloudpulse.dto.Ec2InstanceResponse;
import com.gayoung.cloudpulse.dto.RecommendationResponse;
import com.gayoung.cloudpulse.dto.SecurityGroupCheckResponse;
import com.gayoung.cloudpulse.dto.SecurityGroupRisk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {

    private final Ec2Service ec2Service;
    private final CloudWatchService cloudWatchService;
    private final SecurityGroupService securityGroupService;

    public RecommendationService(
            Ec2Service ec2Service,
            CloudWatchService cloudWatchService,
            SecurityGroupService securityGroupService
    ) {
        this.ec2Service = ec2Service;
        this.cloudWatchService = cloudWatchService;
        this.securityGroupService = securityGroupService;
    }

    public List<RecommendationResponse> getRecommendations() {
        List<Ec2InstanceResponse> instances = ec2Service.getInstances();
        List<CpuMetricResponse> cpuMetrics = cloudWatchService.getCpuMetrics();
        List<SecurityGroupCheckResponse> securityGroups = securityGroupService.checkSecurityGroups();

        return createRecommendations(instances, cpuMetrics, securityGroups);
    }

    public List<RecommendationResponse> createRecommendations(
            List<Ec2InstanceResponse> instances,
            List<CpuMetricResponse> cpuMetrics,
            List<SecurityGroupCheckResponse> securityGroups
    ) {
        List<RecommendationResponse> recommendations = new ArrayList<>();

        for (Ec2InstanceResponse instance : instances) {
            CpuMetricResponse cpuMetric = findCpuMetric(
                    cpuMetrics,
                    instance.instanceId(),
                    instance.region()
            );

            addEc2AndCpuRecommendations(recommendations, instance, cpuMetric);
        }

        for (SecurityGroupCheckResponse securityGroup : securityGroups) {
            addSecurityGroupRecommendations(recommendations, securityGroup);
        }

        return recommendations;
    }

    private CpuMetricResponse findCpuMetric(
            List<CpuMetricResponse> cpuMetrics,
            String instanceId,
            String region
    ) {
        return cpuMetrics.stream()
                .filter(cpu -> cpu.instanceId().equals(instanceId))
                .filter(cpu -> cpu.region().equals(region))
                .findFirst()
                .orElse(null);
    }

    private void addEc2AndCpuRecommendations(
            List<RecommendationResponse> recommendations,
            Ec2InstanceResponse instance,
            CpuMetricResponse cpuMetric
    ) {
        String instanceId = instance.instanceId();
        String instanceName = instance.instanceName() != null ? instance.instanceName() : instanceId;

        if ("stopped".equalsIgnoreCase(instance.state())) {
            recommendations.add(new RecommendationResponse(
                    "COST",
                    instanceName,
                    instanceId,
                    "MEDIUM",
                    "중지된 인스턴스 발견",
                    "EC2 인스턴스가 stopped 상태입니다.",
                    "장기간 사용하지 않는 인스턴스라면 삭제하거나, 필요한 경우에만 다시 시작하는 것을 검토하세요."
            ));

            recommendations.add(new RecommendationResponse(
                    "OPERATION",
                    instanceName,
                    instanceId,
                    "MEDIUM",
                    "서비스 중단 가능성",
                    "EC2 인스턴스가 stopped 상태입니다.",
                    "의도적으로 중지한 인스턴스인지 확인하세요. 운영 중인 서비스라면 즉시 상태 확인이 필요합니다."
            ));

            return;
        }

        if (cpuMetric == null) {
            recommendations.add(new RecommendationResponse(
                    "OPERATION",
                    instanceName,
                    instanceId,
                    "LOW",
                    "CloudWatch CPU 데이터 없음",
                    "해당 인스턴스의 CPU 지표 데이터를 찾을 수 없습니다.",
                    "CloudWatch 지표 수집 여부, 인스턴스 상태, 리전 정보를 확인하세요."
            ));
            return;
        }

        double averageCpu = cpuMetric.averageCpu();
        double maxCpu = cpuMetric.maxCpu();

        if (averageCpu <= 5.0) {
            recommendations.add(new RecommendationResponse(
                    "COST",
                    instanceName,
                    instanceId,
                    "MEDIUM",
                    "저사용 인스턴스 발견",
                    "최근 1시간 평균 CPU 사용률이 " + averageCpu + "%입니다.",
                    "일시적인 저사용일 수 있으므로 장기 추이를 추가로 확인한 뒤, 다운사이징 또는 중지를 검토하세요."
            ));
        }

        if (instance.hasPublicIp() && averageCpu <= 5.0) {
            recommendations.add(new RecommendationResponse(
                    "COST",
                    instanceName,
                    instanceId,
                    "MEDIUM",
                    "Public IP가 있는 저사용 인스턴스",
                    "Public IP가 할당되어 있고 CPU 사용률이 낮습니다.",
                    "외부 접근이 꼭 필요한 인스턴스인지 확인하고, 불필요하다면 Public IP 제거 또는 인스턴스 중지를 검토하세요."
            ));
        }

        if (averageCpu >= 90.0) {
            recommendations.add(new RecommendationResponse(
                    "OPERATION",
                    instanceName,
                    instanceId,
                    "CRITICAL",
                    "CPU 사용률 즉시 조치 필요",
                    "최근 1시간 평균 CPU 사용률이 " + averageCpu + "%입니다.",
                    "Auto Scaling, Load Balancer 적용 또는 인스턴스 타입 상향을 검토하세요."
            ));
        } else if (averageCpu >= 70.0 || maxCpu >= 90.0) {
            recommendations.add(new RecommendationResponse(
                    "OPERATION",
                    instanceName,
                    instanceId,
                    "HIGH",
                    "CPU 과부하 위험",
                    "최근 1시간 평균 CPU 사용률이 " + averageCpu + "%, 최대 CPU 사용률이 " + maxCpu + "%입니다.",
                    "트래픽 증가 또는 애플리케이션 부하 여부를 확인하고, 스케일 아웃 또는 인스턴스 타입 상향을 검토하세요."
            ));
        }
    }

    private void addSecurityGroupRecommendations(
            List<RecommendationResponse> recommendations,
            SecurityGroupCheckResponse securityGroup
    ) {
        String sgName = securityGroup.securityGroupName();
        String sgId = securityGroup.securityGroupId();

        for (SecurityGroupRisk risk : securityGroup.risks()) {
            recommendations.add(new RecommendationResponse(
                    "SECURITY",
                    sgName,
                    sgId,
                    risk.severity(),
                    risk.type(),
                    risk.message(),
                    createSecurityRecommendation(risk)
            ));
        }
    }

    private String createSecurityRecommendation(SecurityGroupRisk risk) {
        String type = risk.type();

        if (type.contains("SSH")) {
            return "SSH 접근이 전체 인터넷에 공개되어 있습니다. 특정 관리자 IP만 허용하거나 Bastion Host를 통해 접근하도록 제한하세요.";
        }

        if (type.contains("RDP")) {
            return "RDP 접근이 전체 인터넷에 공개되어 있습니다. 특정 관리자 IP만 허용하도록 제한하세요.";
        }

        if (type.contains("MySQL") || type.contains("PostgreSQL") || type.contains("DB")) {
            return "DB 포트가 외부에 공개되어 있습니다. 외부 공개를 차단하고 내부 VPC 또는 특정 Security Group에서만 접근하도록 제한하세요.";
        }

        if (type.contains("Redis")) {
            return "Redis 포트가 외부에 공개되어 있습니다. Security Group 내부 접근만 허용하도록 제한하세요.";
        }

        if (type.contains("Elasticsearch") || type.contains("OpenSearch")) {
            return "검색 엔진 포트가 외부에 공개되어 있습니다. 인증 설정을 확인하고 내부 접근만 허용하도록 제한하세요.";
        }

        if (type.contains("PUBLIC_IP_EXPOSED")) {
            return "외부 접근이 꼭 필요한 경우에만 Public IP를 유지하고, Security Group 인바운드 규칙을 최소화하세요.";
        }

        if (type.contains("UNUSED_SECURITY_GROUP")) {
            return "실제로 사용하지 않는 Security Group이라면 삭제하고, 필요한 경우에만 유지하세요.";
        }

        return "Security Group 인바운드 규칙을 확인하고, 불필요하게 전체 공개된 접근을 제거하세요.";
    }
}