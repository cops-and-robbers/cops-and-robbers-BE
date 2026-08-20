package com.team.cops_and_robbers.user.repository;

public record UserNicknameProjection(
        Long userId,
        String nickname
) {}
