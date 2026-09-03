package com.gayoung.cloudpulse.service;

import com.gayoung.cloudpulse.dto.Ec2InstanceResponse;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.InstanceType;
import software.amazon.awssdk.services.ec2.model.Placement;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Ec2ServiceTest {
    private final Ec2ClientFactory factory = new Ec2ClientFactory();
    private final Ec2Service ec2Service = new Ec2Service(factory);

    @Test
    void EC2_Instance_정보를_DTO로_정상_변환한다() {

        // given
        Instance instance = Instance.builder()
                .instanceId("i-1234567890")
                .state(
                        InstanceState.builder()
                                .name(InstanceStateName.RUNNING)
                                .build()
                )
                .instanceType(InstanceType.T3_MICRO)
                .publicIpAddress("3.36.100.10")
                .securityGroups(
                        GroupIdentifier.builder()
                                .groupName("web-sg")
                                .groupId("sg-001")
                                .build()
                )
                .placement(
                        Placement.builder()
                                .availabilityZone("ap-northeast-2a")
                                .build()
                )
                .tags(
                        Tag.builder()
                                .key("Name")
                                .value("web-server")
                                .build(),
                        Tag.builder()
                                .key("Environment")
                                .value("production")
                                .build()
                )
                .build();

        String regionName = "ap-northeast-2";

        // when
        Ec2InstanceResponse dto =
                ec2Service.convertToDto(instance, regionName);

        // then
        assertEquals("i-1234567890", dto.instanceId());
        assertEquals("web-server", dto.instanceName());
        assertEquals("running", dto.state());
        assertEquals("t3.micro", dto.instanceType());

        assertTrue(dto.hasPublicIp());
        assertEquals("3.36.100.10", dto.publicIp());

        assertEquals(List.of("web-sg"), dto.securityGroups());

        assertEquals("ap-northeast-2", dto.region());
        assertEquals("ap-northeast-2a", dto.availabilityZone());
        assertEquals("production", dto.environment());
    }

    @Test
    void Name_태그가_없으면_instanceName은_null이다() {

        // given
        Instance instance = createBaseInstance()
                .tags(
                        Tag.builder()
                                .key("Environment")
                                .value("dev")
                                .build()
                )
                .build();

        // when
        Ec2InstanceResponse dto =
                ec2Service.convertToDto(instance, "ap-northeast-2");

        // then
        assertNull(dto.instanceName());
    }

    @Test
    void Environment_태그가_없으면_environment는_null이다() {

        // given
        Instance instance = createBaseInstance()
                .tags(
                        Tag.builder()
                                .key("Name")
                                .value("test-server")
                                .build()
                )
                .build();

        // when
        Ec2InstanceResponse dto =
                ec2Service.convertToDto(instance, "ap-northeast-2");

        // then
        assertNull(dto.environment());
    }

    @Test
    void Public_IP가_없으면_publicIp는_null이고_hasPublicIp는_false이다() {

        // given
        Instance instance = createBaseInstance()
                .publicIpAddress(null)
                .build();

        // when
        Ec2InstanceResponse dto =
                ec2Service.convertToDto(instance, "ap-northeast-2");

        // then
        assertNull(dto.publicIp());
        assertFalse(dto.hasPublicIp());
    }

    @Test
    void Security_Group이_여러_개이면_모두_DTO에_저장된다() {

        // given
        Instance instance = createBaseInstance()
                .securityGroups(
                        GroupIdentifier.builder()
                                .groupName("web-sg")
                                .build(),

                        GroupIdentifier.builder()
                                .groupName("database-sg")
                                .build(),

                        GroupIdentifier.builder()
                                .groupName("monitoring-sg")
                                .build()
                )
                .build();

        // when
        Ec2InstanceResponse dto =
                ec2Service.convertToDto(instance, "ap-northeast-2");

        // then
        assertEquals(3, dto.securityGroups().size());

        assertTrue(dto.securityGroups().contains("web-sg"));
        assertTrue(dto.securityGroups().contains("database-sg"));
        assertTrue(dto.securityGroups().contains("monitoring-sg"));
    }


    /*
     * 테스트마다 공통으로 필요한 Instance 정보를 만드는 메서드
     */
    private Instance.Builder createBaseInstance() {

        return Instance.builder()
                .instanceId("i-test123")
                .state(
                        InstanceState.builder()
                                .name(InstanceStateName.RUNNING)
                                .build()
                )
                .instanceType(InstanceType.T3_MICRO)
                .publicIpAddress("1.2.3.4")
                .placement(
                        Placement.builder()
                                .availabilityZone("ap-northeast-2a")
                                .build()
                )
                .securityGroups(
                        GroupIdentifier.builder()
                                .groupName("default-sg")
                                .build()
                );
    }
}