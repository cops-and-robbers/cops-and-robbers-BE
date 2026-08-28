package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostNotificationSettingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "CommunityPostNotificationSetting", description = "커뮤니티 게시글별 알림 설정 API")
public interface CommunityPostNotificationSettingControllerDocs {

    @Operation(summary = "게시글 알림 켜기/끄기",
            description = """
                    이 글에서 알림을 받을지 여부를 저장합니다. 토글을 건드린 글만 설정이 남고, 건드리지 않은 글은 아래 기본값을 따릅니다.

                    | 상황 | notifyComments | notifyReplies |
                    |---|---|---|
                    | 내가 쓴 글 | true | false |
                    | 내가 답글의 부모 댓글을 쓴 글 | true | true |
                    | 그 외 (댓글만 단 글 등) | false | false |

                    이미 쌓인 알림에는 영향을 주지 않습니다. 켜더라도 켜기 전에 달린 댓글이 알림함에 소급되지 않습니다.
                    """)
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "설정 성공 (응답 본문 없음)")})
    ResponseEntity<Void> updateNotificationSetting(
            @Parameter(hidden = true) LoginUser loginUser,
            @Parameter(description = "게시글 ID", example = "1") Long postId,
            CommunityPostNotificationSettingRequest request
    );
}
