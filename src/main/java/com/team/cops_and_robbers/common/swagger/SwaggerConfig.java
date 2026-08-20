package com.team.cops_and_robbers.common.swagger;

import com.team.cops_and_robbers.common.exception.ErrorResponse;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        SecurityRequirement securityRequirement = new SecurityRequirement().addList("JWT");

        SecurityScheme securityScheme = new SecurityScheme()
                .name("JWT")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .description("JWT Access Token을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.");

        ResolvedSchema resolvedSchema = ModelConverters.getInstance()
                .resolveAsResolvedSchema(new AnnotatedType(ErrorResponse.class));

        Info info = new Info()
                .title("👮 경찰과 도둑 API 🥷")
                .version("2.17.0")
                .description("""
                        ## v2.17.0 업데이트 내역

                        ### ⚠️ 주의 (클라이언트 동시 배포 필요)
                        - GET /api/community-posts 목록이 국가별로 분리됨
                          - countryCode 또는 현재 위치(latitude, longitude) 중 하나는 필수
                          - 둘 다 없으면 400(COUNTRY_NOT_SPECIFIED)
                          - 좌표로 보내면 서버가 국가를 판별해 응답의 countryCode에 실어줌
                          - 다음 페이지부터는 그 countryCode를 그대로 넣어 좌표 없이 요청
                          - 위치 권한을 거부한 사용자는 기기 국가 코드를 보내면 됨
                        - GET /api/community-posts 페이지네이션을 커서 방식으로 전환
                          - page 파라미터 제거, cursor 파라미터 추가
                          - 응답의 page 객체가 cursor 객체(nextCursor, hasNext)로 변경
                          - 허용 파라미터는 cursor, size, scope, sort 뿐
                          - 지원하지 않는 쿼리 파라미터를 보내면 400(INVALID_QUERY_PARAMETER)

                        ### ✨ 신규
                        - GET /api/community-posts/address 좌표 주소 조회 API 추가 (로그인 필요)
                          - 파라미터: latitude, longitude
                          - 응답: region(동 단위, 게시글에 저장될 값), address(번지 포함, 작성자 확인용),
                            countryCode(ISO 3166-1 alpha-2)
                          - 작성 화면에서 핀을 찍은 직후 호출해 위치를 확인시키는 용도이며 저장하지 않는다
                          - 주소 없는 좌표는 400(ADDRESS_NOT_FOUND), 조회 실패는 500(ADDRESS_LOOKUP_FAILED)
                        - GET /api/community-posts scope, sort 파라미터 추가
                          - scope: ALL | NEARBY | MINE (기본 ALL)
                          - sort: LATEST | POPULAR | DISTANCE | DEADLINE (기본 LATEST)
                          - 현재 구현은 ALL + LATEST 뿐이며 그 외 값은 400
                            (UNSUPPORTED_LIST_SCOPE / UNSUPPORTED_LIST_SORT)
                          - NEARBY 반경 기준, MINE 정의, 인기·거리·마감임박 정렬은 정책 확정 후 지원 예정
                        - 게시글 응답에 작성자 닉네임 writerNickname 추가 (탈퇴한 작성자면 null)
                        - 게시글 응답에 location.region 추가 (동 단위 지역)
                          - 작성/수정 시 좌표를 서버에서 1회 역지오코딩하여 저장
                          - 예: 서울특별시 광진구 군자동 (번지 제외)
                          - 역지오코딩 실패 시 null
                        - 게시글 요청/응답에 location.placeName 추가 (작성자가 입력하는 만나는 곳)
                          - 필수 입력, 최대 50자. 예: 어린이대공원 정문
                          - 좌표로는 건물명·장소명을 신뢰할 수준으로 얻을 수 없어 작성자에게 받는다

                        - 게시글 응답에 location.countryCode 추가, 목록 응답에 countryCode 추가

                        ### 🛠 변경
                        - 해외 주소를 그 나라 언어로 저장 (일본은 일본어, 그 외는 영어)
                        - 역지오코딩 제공자를 카카오에서 VWorld(국내), Geoapify(해외)로 교체
                          - 카카오는 운영정책상 변환 결과를 DB에 저장할 수 없고 해외 좌표를 지원하지 않음
                          - 국내를 먼저 조회하고 주소를 얻지 못하면 해외로 넘어가므로 해외 게시글도 주소가 채워짐

                        ## v2.16.0 업데이트 내역

                        ### 🛠 변경
                        - GET /api/community-posts, GET /api/community-posts/{postId} 비로그인 조회 허용
                          - 웹뷰 지원을 위해 @AuthUser 제거

                        ## v2.15.0 업데이트 내역

                        ### ✨ 신규
                        - 커뮤니티 게시판(같이 하기) CRUD API 추가
                          - POST /api/community-posts 게시글 생성
                          - GET /api/community-posts 게시글 목록 조회 (페이지네이션)
                          - GET /api/community-posts/{postId} 게시글 단건 조회
                          - PUT /api/community-posts/{postId} 게시글 수정
                          - DELETE /api/community-posts/{postId} 게시글 삭제
                          - PATCH /api/community-posts/{postId}/status 모집 상태 변경

                        ## v2.14.0 업데이트 내역

                        ### ✨ 신규
                        - GET /api/notices category 필터 파라미터 추가
                          - NOTICE | MAINTENANCE | EVENT | UPDATE
                          - 생략 시 전체 조회 (기존 동작 유지)
                        - POST /api/notices, PUT /api/notices/{id} category 필드 추가
                          - 생략 또는 null 시 NOTICE로 자동 설정 (하위 호환)

                        ## v2.13.0 업데이트 내역

                        ### ✨ 신규
                        - PUT /api/games/{gameId}/area 폴리곤 구역 타입 지원
                          - areaType 필드 추가 (CIRCLE | POLYGON)
                          - CIRCLE: 기존 원형 구역 (circle 객체 사용)
                          - POLYGON: 다각형 구역 (polygon 객체 사용)
                        - GET /api/games/{gameId}/area 응답 구조 변경
                          - 기존 플랫 구조 → areaType + circle/polygon 중첩 구조

                        ## v2.12.0 업데이트 내역

                        ### ✨ 신규
                        - GET /api/games/{gameId} 응답에 isEventGame 필드 추가
                          - 이벤트 게임 여부를 클라이언트에서 구분할 수 있도록 지원

                        ## v2.11.0 업데이트 내역

                        ### ✨ 신규
                        - POST /api/games/join 이벤트 게임 QR 입장 지원
                          - 진행 중인 이벤트 게임에 초대 코드로 입장 시 경찰로 즉시 합류
                          - 응답에 isEventGame 필드 추가 (클라이언트 인게임/로비 화면 분기용)

                        ## v2.10.0 업데이트 내역

                        ### 🛠 변경 및 수정
                        - DELETE /api/games/{gameId}/leave API 인게임 중도 퇴장 분기처리 추가
                          (제세한 변경 내용은 해당 항목 참고)
                        """);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme)
                        .addSchemas("ErrorResponse", resolvedSchema.schema)
                );    }
}
