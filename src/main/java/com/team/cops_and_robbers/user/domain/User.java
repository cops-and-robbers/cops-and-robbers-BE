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
                        columnNames = {"socialId", "socialType"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SocialType socialType;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(nullable = false)
    private String deviceId;

    private String fcmToken;

    @Column(nullable = false)
    private boolean allowGamePush;

    @Column(nullable = false)
    private boolean allowMarketingPush;
}
