package com.gayoung.cloudpulse.service;

import com.gayoung.cloudpulse.dto.Ec2InstanceResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Reservation;

import java.util.ArrayList;
import java.util.List;

@Service
public class Ec2Service {

    public List<Ec2InstanceResponse> getInstances() {
        List<Ec2InstanceResponse> result = new ArrayList<>();

        try (Ec2Client defaultClient = Ec2Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .build()) {

            List<String> regions = defaultClient.describeRegions()
                    .regions()
                    .stream()
                    .map(region -> region.regionName())
                    .toList();

            for (String regionName : regions) {
                try (Ec2Client regionalClient = Ec2Client.builder()
                        .region(Region.of(regionName))
                        .build()) {

                    regionalClient.describeInstancesPaginator()
                            .stream()
                            .flatMap(response -> response.reservations().stream())
                            .flatMap(reservation -> reservation.instances().stream())
                            .forEach(instance -> {

                                List<String> securityGroups = instance.securityGroups()
                                        .stream()
                                        .map(sg -> sg.groupName())
                                        .toList();

                                String instanceName = instance.tags()
                                        .stream()
                                        .filter(tag -> tag.key().equals("Name"))
                                        .map(tag -> tag.value())
                                        .findFirst()
                                        .orElse(null);

                                String environment = instance.tags()
                                        .stream()
                                        .filter(tag -> tag.key().equals("Environment"))
                                        .map(tag -> tag.value())
                                        .findFirst()
                                        .orElse(null);

                                String publicIp = instance.publicIpAddress();
                                boolean hasPublicIp = publicIp != null && !publicIp.isBlank();

                                result.add(new Ec2InstanceResponse(
                                        instance.instanceId(),
                                        instanceName,
                                        instance.state().nameAsString(),
                                        instance.instanceTypeAsString(),
                                        hasPublicIp,
                                        publicIp,
                                        securityGroups,
                                        regionName,
                                        instance.placement().availabilityZone(),
                                        environment
                                ));
                            });

                } catch (Ec2Exception e) {
                    System.out.println("Failed to describe instances in region: " + regionName);
                }
            }
        }

        return result;
    }
}