package com.gayoung.cloudpulse.service;

import com.gayoung.cloudpulse.dto.Ec2InstanceResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;

import java.util.ArrayList;
import java.util.List;

@Service
public class Ec2Service {
    private final Ec2Client ec2Client;

    public Ec2Service() {
        this.ec2Client = Ec2Client.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

    public List<Ec2InstanceResponse> getInstances() {
        DescribeInstancesResponse response = ec2Client.describeInstances();

        List<Ec2InstanceResponse> result = new ArrayList<>();

        for (Reservation reservation : response.reservations()) {
            for (Instance instance : reservation.instances()) {
                result.add(new Ec2InstanceResponse(
                        instance.instanceId(),
                        instance.state().nameAsString(),
                        instance.instanceTypeAsString(),
                        instance.publicIpAddress()
                ));
            }
        }

        return result;
    }
}
