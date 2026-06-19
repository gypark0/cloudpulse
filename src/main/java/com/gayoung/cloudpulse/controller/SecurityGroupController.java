package com.gayoung.cloudpulse.controller;

import com.gayoung.cloudpulse.dto.SecurityGroupCheckResponse;
import com.gayoung.cloudpulse.service.SecurityGroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SecurityGroupController {

    private final SecurityGroupService securityGroupService;

    public SecurityGroupController(SecurityGroupService securityGroupService) {
        this.securityGroupService = securityGroupService;
    }

    @GetMapping("/api/security-groups/check")
    public List<SecurityGroupCheckResponse> checkSecurityGroups() {
        return securityGroupService.checkSecurityGroups();
    }
}