package com.team.cops_and_robbers.community.post.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.post.application.dto.command.CommunityPostUpdateCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_posts")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost extends BaseTimeEntity {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long writerId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime meetingAt;

    @Column(columnDefinition = "GEOMETRY(POINT, 4326)", nullable = false)
    private Point location;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RecruitmentStatus status = RecruitmentStatus.RECRUITING;

    @Column(length = 255)
    private String address;

    @Column(length = 255)
    private String roadAddress;

    @Column(length = 255)
    private String buildingName;

    @Column(length = 255)
    private String region;

    @Column(nullable = false, length = 2)
    private String countryCode;

    @Column(nullable = false, length = 50)
    private String placeName;

    public static CommunityPost createPost(CommunityPostCreateCommand command, PostAddress postAddress) {
        return CommunityPost.builder()
                .writerId(command.writerId())
                .title(command.title())
                .content(command.content())
                .meetingAt(command.meetingAt())
                .location(toPoint(command.latitude(), command.longitude()))
                .maxParticipants(command.maxParticipants())
                .placeName(command.placeName())
                .address(postAddress.address())
                .roadAddress(postAddress.roadAddress())
                .buildingName(postAddress.buildingName())
                .region(postAddress.region())
                .countryCode(postAddress.countryCode())
                .build();
    }

    public void updatePost(CommunityPostUpdateCommand command, PostAddress postAddress) {
        this.title = command.title();
        this.content = command.content();
        this.meetingAt = command.meetingAt();
        this.location = toPoint(command.latitude(), command.longitude());
        this.maxParticipants = command.maxParticipants();
        this.placeName = command.placeName();
        this.address = postAddress.address();
        this.roadAddress = postAddress.roadAddress();
        this.buildingName = postAddress.buildingName();
        this.region = postAddress.region();
        this.countryCode = postAddress.countryCode();
    }

    public Double getLatitude() {
        return location.getY();
    }

    public Double getLongitude() {
        return location.getX();
    }

    private static Point toPoint(Double latitude, Double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    public PostAddress getPostAddress() {
        return PostAddress.of(address, roadAddress, buildingName, region, countryCode);
    }

    public void updateStatus(RecruitmentStatus status) {
        this.status = status;
    }

    /** 날짜가 지나면 ENDED로 본다. 저장된 status는 그대로 두고 조회 시점에 판정한다. */
    public RecruitmentStatus currentStatus() {
        if (meetingAt.isBefore(LocalDateTime.now())) {
            return RecruitmentStatus.ENDED;
        }
        return status;
    }

    public boolean isClosed() {
        return currentStatus().isClosed();
    }
}
