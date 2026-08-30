package com.team.cops_and_robbers.community.post.domain;

public enum RecruitmentStatus {

    RECRUITING,
    COMPLETED,
    ENDED;

    public boolean isClosed() {
        return this != RECRUITING;
    }
}
