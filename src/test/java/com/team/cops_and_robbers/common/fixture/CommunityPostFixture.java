package com.team.cops_and_robbers.common.fixture;

import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.RecruitmentStatus;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class CommunityPostFixture {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    public static Point point(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    public static CommunityPost POST(Long userId) {
        CommunityPost post = CommunityPost.builder()
                .writerId(userId)
                .title("같이 경찰과 도둑 하실 분!")
                .content("강남역 근처에서 5명 모집합니다.")
                .meetingAt(LocalDateTime.now().plusDays(3))
                .location(point(37.4979, 127.0276))
                .maxParticipants(6)
                .placeName("어린이대공원 정문")
                .countryCode("KR")
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

    public static CommunityPost PAST_POST(Long userId) {
        CommunityPost post = POST(userId);
        ReflectionTestUtils.setField(post, "meetingAt", LocalDateTime.now().minusDays(1));
        return post;
    }

    public static CommunityPost POST_TITLED(Long userId, String title) {
        CommunityPost post = POST(userId);
        ReflectionTestUtils.setField(post, "title", title);
        return post;
    }

    public static CommunityPost POST_PLACED(Long userId, String region, String placeName) {
        CommunityPost post = POST(userId);
        ReflectionTestUtils.setField(post, "region", region);
        ReflectionTestUtils.setField(post, "placeName", placeName);
        return post;
    }

    public static CommunityPost POST_AT(Long userId, double latitude, double longitude) {
        CommunityPost post = POST(userId);
        ReflectionTestUtils.setField(post, "location", point(latitude, longitude));
        return post;
    }

    public static CommunityPost POST_MEETING_IN(Long userId, long days) {
        CommunityPost post = POST(userId);
        ReflectionTestUtils.setField(post, "meetingAt", LocalDateTime.now().plusDays(days));
        return post;
    }

    public static CommunityPost PAST_COMPLETED_POST(Long userId) {
        CommunityPost post = COMPLETED_POST(userId);
        ReflectionTestUtils.setField(post, "meetingAt", LocalDateTime.now().minusDays(1));
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
                .location(point(37.5665, 126.9780))
                .maxParticipants(4)
                .placeName("광화문광장 세종대왕상")
                .countryCode("KR")
                .status(RecruitmentStatus.COMPLETED)
                .build();
        ReflectionTestUtils.setField(post, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(post, "updatedAt", LocalDateTime.now());
        return post;
    }
}
