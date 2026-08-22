package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.application.dto.CommunityPostRow;
import com.team.cops_and_robbers.community.application.dto.CommunityPostSearchCondition;
import java.util.List;

public interface CommunityPostRepositoryCustom {

    List<CommunityPostRow> findPage(CommunityPostSearchCondition condition, CommunityPostCursor cursor, int size);
}
