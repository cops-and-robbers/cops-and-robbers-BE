package com.team.cops_and_robbers.user.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_devices")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeviceType deviceType;

    @Column(nullable = false)
    private String fcmToken;


    public static UserDevice connect(User user, String deviceId,
                                  DeviceType deviceType, String fcmToken
    ) {
        return UserDevice.builder()
                .user(user)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .fcmToken(fcmToken)
                .build();
    }

    public void reconnect(String deviceId, DeviceType deviceType, String fcmToken) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.fcmToken = fcmToken;
    }

}
