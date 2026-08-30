package com.team.cops_and_robbers.community.notification.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.notification.presentation.dto.response.CommunityNotificationListResponse;
import com.team.cops_and_robbers.community.notification.presentation.dto.response.CommunityNotificationUnreadCountResponse;
import com.team.cops_and_robbers.user.exception.UserException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CommunityNotification", description = "커뮤니티 알림함 API")
public interface CommunityNotificationControllerDocs {

    @Operation(summary = "알림함 목록 조회",
            description = """
                    내 알림을 최신순으로 조회합니다. 최근 60일 이내 알림만 내려갑니다.

                    조회만으로는 읽음 처리되지 않습니다. 읽음은 `POST /api/community-posts/notifications/read`로 따로 보내주세요.
                    `read`는 알림마다 저장하는 값이 아니라 유저당 하나인 읽음 커서로 판정한 결과입니다.
                    """)
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "조회 성공")})
    ResponseEntity<CommunityNotificationListResponse> getNotifications(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "이전 응답의 nextCursor. 첫 페이지는 생략", example = "12") Long cursor,
            @Parameter(description = "페이지 크기 (1~50)", example = "20") int size
    );

    @Operation(summary = "안 읽은 알림 개수 조회",
            description = """
                    알림함 배지에 쓸 개수입니다. 최근 60일 이내 알림 중 읽음 커서보다 나중에 생긴 것만 셉니다.
                    """)
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "조회 성공")})
    ResponseEntity<CommunityNotificationUnreadCountResponse> getUnreadCount(
            @Parameter(hidden = true) LoginUser loginUser
    );

    @Operation(summary = "알림 읽음 처리",
            description = """
                    읽음 커서를 현재 시각으로 옮겨 지금까지의 알림을 모두 읽음으로 만듭니다.
                    알림을 개별로 읽음 처리하지는 않습니다.
                    """)
    @ApiErrorCode(value = UserException.class, codes = {"USER_NOT_FOUND"})
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "읽음 처리 성공")})
    ResponseEntity<Void> readNotifications(
            @Parameter(hidden = true) LoginUser loginUser
    );
}
