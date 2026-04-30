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
    private boolean allowGamePush;

    @Column(nullable = false)
    private boolean allowMarketingPush;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static User signUp(String socialId, SocialType socialType, String nickname) {
        return User.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname(nickname)
                .allowGamePush(true)
                .allowMarketingPush(false)
                .role(Role.USER)
                .build();
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public void updateNickname(String modifiedNickname) {
        this.nickname = modifiedNickname;
    }

    public boolean hasSameNickname(String modifiedNickname) {
        return this.nickname.equals(modifiedNickname);
    }
}
