package com.team.cops_and_robbers.community.chat.common.repository;

public record CommunityChatSenderProfileProjection(
        String nickname,
        int profileIcon,
        boolean requiredTermsAgreed
) {}
