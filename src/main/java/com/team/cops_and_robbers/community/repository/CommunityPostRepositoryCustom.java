package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import java.util.List;

public interface CommunityPostRepositoryCustom {

    List<CommunityPost> findPage(String countryCode, CommunityPostSort sort, CommunityPostCursor cursor, int size);
}
