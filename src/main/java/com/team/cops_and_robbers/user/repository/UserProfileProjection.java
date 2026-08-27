package com.team.cops_and_robbers.user.repository;

public record UserProfileProjection(
        Long userId,
        String nickname,
        int profileIcon
) {}
