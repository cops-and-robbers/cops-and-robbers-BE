package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class CommunityPostFixture {

    public static CommunityPost POST(Long userId) {
        CommunityPost post = CommunityPost.builder()
                .writerId(userId)
                .title("같이 경찰과 도둑 하실 분!")
                .content("강남역 근처에서 5명 모집합니다.")
                .meetingAt(LocalDateTime.now().plusDays(3))
                .latitude(37.4979)
                .longitude(127.0276)
                .maxParticipants(6)
                .placeName("어린이대공원 정문")
                .build();
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        return post;
    }

    public static CommunityPost POST(Long userId, String address) {
        CommunityPost post = POST(userId);
        ReflectionTestUtils.setField(post, "address", address);
        return post;
    }

    public static CommunityPost POST(Long userId, LocalDateTime createdAt) {
        CommunityPost post = POST(userId);
        ReflectionTestUtils.setField(post, "createdAt", createdAt);
        return post;
    }

    public static CommunityPost COMPLETED_POST(Long userId) {
        CommunityPost post = CommunityPost.builder()
                .writerId(userId)
                .title("모집 완료된 게시글")
                .content("모집이 완료되었습니다.")
                .meetingAt(LocalDateTime.now().plusDays(1))
                .latitude(37.5665)
                .longitude(126.9780)
                .maxParticipants(4)
                .placeName("광화문광장 세종대왕상")
                .status(RecruitmentStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        return post;
    }
}
