package com.team.cops_and_robbers.community.repository;

import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import java.util.List;

public interface CommunityPostRepositoryCustom {

    List<CommunityPost> findPage(String countryCode, CommunityPostCursor cursor, int size);
}
