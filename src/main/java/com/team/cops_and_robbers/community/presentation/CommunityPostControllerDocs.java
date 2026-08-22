package com.team.cops_and_robbers.community.presentation;

import com.team.cops_and_robbers.auth.presentation.resolver.LoginUser;
import com.team.cops_and_robbers.common.swagger.ApiErrorCode;
import com.team.cops_and_robbers.community.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.exception.CommunityPostException;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostCreateRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostStatusRequest;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityPostUpdateRequest;
import com.team.cops_and_robbers.community.presentation.dto.response.AddressResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostListResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CountryResponse;
import com.team.cops_and_robbers.community.presentation.dto.response.CommunityPostResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "CommunityPost", description = "커뮤니티 모집 게시글 API")
public interface CommunityPostControllerDocs {

    @SecurityRequirements
    @Operation(summary = "좌표 국가 조회 (로그인 불필요)",
            description = "좌표가 속한 국가 코드를 반환합니다. 목록 조회 전에 한 번 호출해 countryCode를 얻는 용도입니다. "
                    + "주소는 만들지 않으므로 표기 언어를 맞추는 추가 호출을 하지 않습니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"COUNTRY_NOT_SPECIFIED", "ADDRESS_LOOKUP_FAILED"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CountryResponse> getCountry(
            @Parameter(description = "위도", example = "37.5502") @RequestParam Double latitude,
            @Parameter(description = "경도", example = "127.0736") @RequestParam Double longitude
    );

    @Operation(summary = "좌표 주소 조회",
            description = "좌표에 해당하는 주소를 조회합니다. 저장하지 않으며, 작성 화면에서 위치를 확인시켜 주기 위한 용도입니다. "
                    + "게시글에는 region이 저장되고 address는 작성자 확인용입니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"ADDRESS_NOT_FOUND", "ADDRESS_LOOKUP_FAILED"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<AddressResponse> getAddress(
            @Parameter(description = "위도", example = "37.5502") @RequestParam Double latitude,
            @Parameter(description = "경도", example = "127.0736") @RequestParam Double longitude
    );

    @Operation(summary = "게시글 생성", description = "새로운 모집 게시글을 생성합니다. 좌표는 서버에서 동 단위 지역으로 변환해 저장하며, 주소를 찾을 수 없는 위치는 400을 반환합니다. 만나는 곳(placeName)은 작성자가 직접 입력합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"INVALID_MEETING_DATE", "ADDRESS_NOT_FOUND", "ADDRESS_LOOKUP_FAILED"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공")
    })
    ResponseEntity<CommunityPostResponse> createPost(
            @Parameter(hidden = true) LoginUser loginUser,
            @RequestBody @Valid CommunityPostCreateRequest request
    );

    @SecurityRequirements
    @Operation(summary = "게시글 목록 조회 (로그인 불필요)",
            description = "모집 게시글 목록을 국가별로 나눠 최신순 커서 방식으로 조회합니다. "
                    + "웹뷰 지원을 위해 인증 없이 호출할 수 있습니다. "
                    + "countryCode는 필수이며, GET /api/community-posts/country 로 먼저 조회하세요. "
                    + "지원하지 않는 쿼리 파라미터가 포함되면 400을 반환합니다.")
    @ApiErrorCode(value = CommunityPostException.class,
            codes = {"UNSUPPORTED_LIST_SCOPE", "UNSUPPORTED_LIST_SORT", "COUNTRY_NOT_SPECIFIED", "ADDRESS_LOOKUP_FAILED"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 커서 / 사이즈 범위 초과 / 미지원 파라미터")
    })
    ResponseEntity<CommunityPostListResponse> getPostList(
            @Parameter(description = "이전 응답의 nextCursor 값 (첫 페이지는 생략)") @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (1~100)", example = "10") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "조회 범위. 현재는 ALL만 지원하며 NEARBY, MINE은 400", example = "ALL")
            @RequestParam(defaultValue = "ALL") CommunityPostScope scope,
            @Parameter(description = "정렬 기준. 현재는 LATEST만 지원하며 POPULAR, DISTANCE, DEADLINE은 400", example = "LATEST")
            @RequestParam(defaultValue = "LATEST") CommunityPostSort sort,
            @Parameter(description = "조회할 국가 코드(ISO 3166-1 alpha-2). 필수", example = "KR")
            @RequestParam String countryCode
    );

    @SecurityRequirements
    @Operation(summary = "게시글 단건 조회 (로그인 불필요)",
            description = "특정 모집 게시글을 조회합니다.\n\n"
                    + "location.region은 동 단위(예: 서울특별시 광진구 군자동), "
                    + "location.address는 번지까지 포함한 지번 주소입니다(예: 서울특별시 광진구 군자동 98). "
                    + "화면에는 region을 쓰고 주소 복사에는 address를 쓰세요.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ResponseEntity<CommunityPostResponse> getPost(
            @PathVariable Long postId
    );

    @Operation(summary = "게시글 수정", description = "특정 모집 게시글을 수정합니다. 좌표가 바뀌었거나 지역 정보가 비어 있으면 주소를 다시 변환합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND", "FORBIDDEN_NOT_AUTHOR", "INVALID_MEETING_DATE",
            "ADDRESS_NOT_FOUND", "ADDRESS_LOOKUP_FAILED"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공")
    })
    ResponseEntity<CommunityPostResponse> updatePost(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostUpdateRequest request
    );

    @Operation(summary = "게시글 삭제", description = "특정 모집 게시글을 삭제합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND", "FORBIDDEN_NOT_AUTHOR"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공 (응답 본문 없음)")
    })
    ResponseEntity<Void> deletePost(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long postId
    );

    @Operation(summary = "모집 상태 변경", description = "모집 게시글의 모집 상태를 변경합니다.")
    @ApiErrorCode(value = CommunityPostException.class, codes = {"POST_NOT_FOUND", "FORBIDDEN_NOT_AUTHOR"})
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 변경 성공")
    })
    ResponseEntity<CommunityPostResponse> updateStatus(
            @Parameter(hidden = true) LoginUser loginUser,
            @PathVariable Long postId,
            @RequestBody @Valid CommunityPostStatusRequest request
    );
}
