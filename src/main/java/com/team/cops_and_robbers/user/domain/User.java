package com.team.cops_and_robbers.user.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_social_id_type",
                        columnNames = {"social_id", "social_type"}
                )
        }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SocialType socialType;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;

    @Column(nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DeviceType deviceType;

    @Column(nullable = false)
    private String fcmToken;

    @Column(nullable = false)
    private boolean allowGamePush;

    @Column(nullable = false)
    private boolean allowMarketingPush;

    public static User signUp(
            String socialId, SocialType socialType, String nickname,
            String deviceId, DeviceType deviceType, String fcmToken
    ) {
        return User.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname(nickname)
                .deviceId(deviceId)
                .deviceType(deviceType)
                .fcmToken(fcmToken)
                .allowGamePush(true)
                .allowMarketingPush(false)
                .build();
    }

    public void updateDeviceAndFcmToken(
            String deviceId, DeviceType deviceType, String fcmToken
    ) {
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.fcmToken = fcmToken;
    }

}
