package com.team.cops_and_robbers.user.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    @Column(nullable = false)
    private boolean termsOfServiceAgreed;

    @Column(nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(nullable = false)
    private boolean locationTermsAgreed;

    private LocalDateTime termsAgreedAt;

    private LocalDateTime marketingAgreedAt;

    public static User signUp(String socialId, SocialType socialType, String nickname) {
        return User.builder()
                .socialId(socialId)
                .socialType(socialType)
                .nickname(nickname)
                .allowGamePush(true)
                .allowMarketingPush(false)
                .role(Role.USER)
                .termsOfServiceAgreed(false)
                .privacyPolicyAgreed(false)
                .locationTermsAgreed(false)
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

    public boolean hasAgreedRequiredTerms() {
        return this.locationTermsAgreed && this.termsOfServiceAgreed && this.privacyPolicyAgreed;
    }

    public void agreeTerms(boolean marketing, LocalDateTime now) {
        agreeRequiredTerms(now);
        updateMarketingPush(marketing, now);
    }

    private void updateMarketingPush(boolean marketing, LocalDateTime now) {
        this.allowMarketingPush = marketing;
        if (!marketing) {
            this.marketingAgreedAt = null;
        } else if (this.marketingAgreedAt == null) {
            this.marketingAgreedAt = now;
        }
    }

    private void agreeRequiredTerms(LocalDateTime now) {
        this.termsOfServiceAgreed = true;
        this.privacyPolicyAgreed = true;
        this.locationTermsAgreed = true;
        if (this.termsAgreedAt == null) {
            this.termsAgreedAt = now;
        }
    }

    public void updateGamePush(boolean allowGamePush) {
        this.allowGamePush = allowGamePush;
    }
}
