package com.team.cops_and_robbers.community.post.repository;

import com.team.cops_and_robbers.community.post.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.post.application.dto.CommunityPostSearchCondition;

import java.util.List;

public interface CommunityPostRepositoryCustom {

    List<CommunityPostRow> findPage(CommunityPostSearchCondition condition, CommunityPostCursor cursor, int size);
}
