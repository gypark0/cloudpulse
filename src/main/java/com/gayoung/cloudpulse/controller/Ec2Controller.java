package com.gayoung.cloudpulse.controller;

import com.gayoung.cloudpulse.dto.Ec2InstanceResponse;
import com.gayoung.cloudpulse.service.Ec2Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class Ec2Controller {
    private final Ec2Service ec2Service;

    public Ec2Controller(Ec2Service ec2Service) {
        this.ec2Service = ec2Service;
    }

    @GetMapping("/api/ec2/instances")
    public List<Ec2InstanceResponse> getInstances() {
        return ec2Service.getInstances();
    }
}
