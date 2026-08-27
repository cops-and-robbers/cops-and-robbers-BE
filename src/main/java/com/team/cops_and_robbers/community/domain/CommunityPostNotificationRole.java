package com.team.cops_and_robbers.community.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 설정 행이 없을 때 적용할 기본값. 그 글에서의 역할에 따라 갈린다. */
@Getter
@RequiredArgsConstructor
public enum CommunityPostNotificationRole {

    POST_WRITER(true, false),
    OTHER(false, false);

    private final boolean notifyComments;
    private final boolean notifyReplies;

    public boolean allows(CommunityNotificationType type) {
        return type == CommunityNotificationType.REPLY ? notifyReplies : notifyComments;
    }
}
