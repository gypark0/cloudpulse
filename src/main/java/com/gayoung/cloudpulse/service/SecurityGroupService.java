package com.gayoung.cloudpulse.service;

import com.gayoung.cloudpulse.dto.SecurityGroupCheckResponse;
import com.gayoung.cloudpulse.dto.SecurityGroupRisk;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.*;

@Service
public class SecurityGroupService {

    private static final Region DEFAULT_REGION = Region.AP_NORTHEAST_2;

    private static final Map<Integer, String> DANGEROUS_PORTS = Map.of(
            22, "SSH",
            3389, "RDP",
            3306, "MySQL",
            5432, "PostgreSQL",
            6379, "Redis",
            9200, "Elasticsearch/OpenSearch"
    );

    public List<SecurityGroupCheckResponse> checkSecurityGroups() {
        List<SecurityGroupCheckResponse> result = new ArrayList<>();

        try (Ec2Client defaultClient = Ec2Client.builder()
                .region(DEFAULT_REGION)
                .build()) {

            DescribeRegionsResponse regionsResponse = defaultClient.describeRegions();

            for (software.amazon.awssdk.services.ec2.model.Region region : regionsResponse.regions()) {
                String regionName = region.regionName();

                try (Ec2Client ec2Client = Ec2Client.builder()
                        .region(Region.of(regionName))
                        .build()) {

                    Map<String, List<String>> sgToInstanceIds = getSecurityGroupToInstanceIds(ec2Client);
                    Set<String> publicIpSecurityGroupIds = getPublicIpSecurityGroupIds(ec2Client);

                    for (DescribeSecurityGroupsResponse page : ec2Client.describeSecurityGroupsPaginator()) {
                        for (SecurityGroup securityGroup : page.securityGroups()) {

                            List<SecurityGroupRisk> risks = new ArrayList<>();

                            checkDangerousInboundRules(securityGroup, risks);

                            String sgId = securityGroup.groupId();

                            boolean attachedToEc2 = sgToInstanceIds.containsKey(sgId);
                            boolean publicIpExposed = publicIpSecurityGroupIds.contains(sgId);
                            boolean unusedCandidate = !attachedToEc2;

                            if (publicIpExposed) {
                                risks.add(new SecurityGroupRisk(
                                        "PUBLIC_IP_EXPOSED",
                                        "Public IP가 할당된 EC2 인스턴스에 연결된 Security Group입니다.",
                                        "MEDIUM"
                                ));
                            }

                            if (unusedCandidate) {
                                risks.add(new SecurityGroupRisk(
                                        "UNUSED_SECURITY_GROUP",
                                        "어떤 EC2 인스턴스에도 연결되어 있지 않은 Security Group입니다.",
                                        "LOW"
                                ));
                            }

                            result.add(new SecurityGroupCheckResponse(
                                    regionName,
                                    sgId,
                                    securityGroup.groupName(),
                                    securityGroup.vpcId(),
                                    attachedToEc2,
                                    publicIpExposed,
                                    unusedCandidate,
                                    sgToInstanceIds.getOrDefault(sgId, List.of()),
                                    risks
                            ));
                        }
                    }

                } catch (Ec2Exception e) {
                    System.out.println("Failed to check region: " + regionName + " / " + e.awsErrorDetails().errorMessage());
                }
            }
        }

        return result;
    }

    private Map<String, List<String>> getSecurityGroupToInstanceIds(Ec2Client ec2Client) {
        Map<String, List<String>> sgToInstanceIds = new HashMap<>();

        for (DescribeInstancesResponse page : ec2Client.describeInstancesPaginator()) {
            for (Reservation reservation : page.reservations()) {
                for (Instance instance : reservation.instances()) {
                    String instanceId = instance.instanceId();

                    for (GroupIdentifier group : instance.securityGroups()) {
                        sgToInstanceIds
                                .computeIfAbsent(group.groupId(), key -> new ArrayList<>())
                                .add(instanceId);
                    }
                }
            }
        }

        return sgToInstanceIds;
    }

    private Set<String> getPublicIpSecurityGroupIds(Ec2Client ec2Client) {
        Set<String> publicIpSgIds = new HashSet<>();

        for (DescribeInstancesResponse page : ec2Client.describeInstancesPaginator()) {
            for (Reservation reservation : page.reservations()) {
                for (Instance instance : reservation.instances()) {

                    if (instance.publicIpAddress() == null) {
                        continue;
                    }

                    for (GroupIdentifier group : instance.securityGroups()) {
                        publicIpSgIds.add(group.groupId());
                    }
                }
            }
        }

        return publicIpSgIds;
    }

    private void checkDangerousInboundRules(SecurityGroup securityGroup, List<SecurityGroupRisk> risks) {
        for (IpPermission permission : securityGroup.ipPermissions()) {
            for (Integer port : DANGEROUS_PORTS.keySet()) {
                if (isPortOpenToWorld(permission, port)) {
                    String serviceName = DANGEROUS_PORTS.get(port);

                    risks.add(new SecurityGroupRisk(
                            serviceName + "_OPEN_TO_WORLD",
                            serviceName + "(" + port + ") 포트가 0.0.0.0/0 또는 ::/0으로 전체 공개되어 있습니다.",
                            "HIGH"
                    ));
                }
            }
        }
    }

    private boolean isPortOpenToWorld(IpPermission permission, int targetPort) {
        boolean portMatched = isPortIncluded(permission, targetPort);
        boolean openToWorld = isOpenToWorld(permission);

        return portMatched && openToWorld;
    }

    private boolean isPortIncluded(IpPermission permission, int targetPort) {
        String protocol = permission.ipProtocol();

        if ("-1".equals(protocol)) {
            return true;
        }

        if (permission.fromPort() == null || permission.toPort() == null) {
            return false;
        }

        return permission.fromPort() <= targetPort && targetPort <= permission.toPort();
    }

    private boolean isOpenToWorld(IpPermission permission) {
        boolean ipv4Open = permission.ipRanges().stream()
                .anyMatch(range -> "0.0.0.0/0".equals(range.cidrIp()));

        boolean ipv6Open = permission.ipv6Ranges().stream()
                .anyMatch(range -> "::/0".equals(range.cidrIpv6()));

        return ipv4Open || ipv6Open;
    }
}