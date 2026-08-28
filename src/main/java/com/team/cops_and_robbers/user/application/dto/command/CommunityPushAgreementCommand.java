package com.team.cops_and_robbers.user.application.dto.command;

public record CommunityPushAgreementCommand(
        Long userId,
        Boolean allowCommunityPush
) {
    public static CommunityPushAgreementCommand of(Long userId, Boolean allowCommunityPush) {
        return new CommunityPushAgreementCommand(userId, allowCommunityPush);
    }
}
