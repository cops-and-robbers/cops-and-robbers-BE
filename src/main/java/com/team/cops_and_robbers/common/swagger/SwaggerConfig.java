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
                .version("2.20.0")
                .description("""
                        ## v2.20.0 업데이트 내역

                        ### ⚠️ 주의 (클라이언트 대응 필요)
                        - 소켓 접속 주소를 /connection 으로 변경
                          - 기존 /game-connection 도 계속 동작한다 (별칭 유지)
                          - 게임용과 커뮤니티용 소켓을 따로 열지 말고 하나로 함께 쓴다
                        - 게임 초대 메시지 전송 형식이 문자열에서 객체로 변경
                          - message에 JSON 문자열을 넣던 방식 → gameInvite 객체로 전달
                          - gameInvite에 inviterNickname은 없다. 초대자는 발신자와 같으므로 senderNickname을 쓴다

                        ### ✨ 신규 — 커뮤니티 채팅 (모집 게시글 단톡방)
                        - POST /api/community-posts/{postId}/chat/join — 채팅방 참여
                          - 모집 중인 게시글에만 참여할 수 있고, 정원과 참여 방 수(100개) 상한이 있다
                        - DELETE /api/community-posts/{postId}/chat/leave — 채팅방 나가기
                          - 작성자는 나갈 수 없다. 나가면 대화 내역을 볼 수 없다
                        - GET /api/community-posts/{postId}/chat/messages — 채팅 내역 조회 (커서 페이지네이션)
                        - GET /api/community-posts/chat/rooms — 내 채팅방 목록 (최근 대화순)
                        - 실시간 송수신은 STOMP로 한다
                          - 구독 /subscribe/community/{postId}/chat, 전송 /publish/community/{postId}/chat
                          - 멤버가 아니면 구독이 거부된다
                        - 메시지 타입은 TEXT / SYSTEM / GAME_INVITE 세 가지
                          - SYSTEM은 서버 전용이라 클라이언트가 보낼 수 없다
                          - SYSTEM·GAME_INVITE 본문에는 닉네임을 넣지 않는다. 화면 문구는 senderNickname으로 조립한다
                        - 게시글을 만들면 작성자가 채팅방 멤버로 자동 등록되고, 게시글을 지우면 대화 내역도 함께 지워진다

                        ### ✨ 신규 — 커뮤니티 목록 정렬
                        - GET /api/community-posts sort=DEADLINE 지원 (모임 날짜 오름차순)
                        - GET /api/community-posts sort=DISTANCE 지원 (사용자 좌표 기준 가까운 순)
                          - latitude, longitude 필수. 없으면 400
                          - 다른 정렬에서 좌표를 보내면 400 (조용히 무시되지 않도록)
                          - POPULAR는 아직 400
                        - 커서에 정렬 종류가 담기므로 받은 정렬과 다른 sort로 보내면 400

                        ### 🛠 변경
                        - 마감된 게시글은 정렬과 무관하게 목록 맨 뒤로
                          - 작성자가 마감(COMPLETED)했거나 모임 날짜가 지난(ENDED) 글
                        - 모임 날짜가 지난 게시글의 status를 ENDED로 내려줌
                          - RECRUITING / COMPLETED / ENDED (ENDED 신규)
                          - 저장하지 않고 조회 시점에 판정한다

                        ## v2.19.0 업데이트 내역

                        ### ✨ 신규
                        - 게시글 응답 location에 address(지번 주소) 추가
                          - 예: 서울특별시 광진구 군자동 98 (region은 번지를 뺀 동 단위)
                          - 화면 표기는 region 그대로 쓰고, 주소 복사에 address를 쓴다
                          - 목록·상세·생성·수정 응답에 모두 내려간다
                          - 역지오코딩으로 지번을 얻지 못한 좌표는 null

                        ### 🛠 변경
                        - 신규 회원 닉네임을 Accept-Language 언어로 생성 (ko / ja / en)
                          - ko: 재빠른고양이3721 / ja: すばやいネコ3721 / en: SpeedyCat3721
                          - 헤더가 없거나 지원하지 않는 언어면 한국어
                          - 대부분의 클라이언트가 기기 설정으로 헤더를 자동 전송한다
                        - 닉네임 낱말을 직업에서 동물로 교체
                          - 직업은 "잠많은형사" 같은 조합이 성립하지 않고, 모욕이 되는 낱말이 섞여 있었다
                        - 닉네임 끝 숫자를 4자리로 고정 (기존에는 패딩이 없어 1~3자리도 생성됨)

                        ## v2.18.0 업데이트 내역

                        ### ⚠️ 주의 (클라이언트 동시 배포 필요)
                        - GET /api/community-posts 에서 latitude, longitude 파라미터 제거
                          - countryCode만 받는다. 보내면 400(INVALID_QUERY_PARAMETER)
                          - 국가는 GET /api/community-posts/country 로 먼저 조회한다
                        - 목록 응답 최상위의 countryCode 제거 (요청값을 되돌려주던 중복)

                        ### ✨ 신규
                        - GET /api/community-posts/country 좌표 국가 조회 API 🔓 no auth
                          - 파라미터: latitude, longitude / 응답: countryCode
                          - 화면 진입 때 한 번 호출해 국가를 얻고, 이후 페이지는 그 값을 재사용한다
                          - 주소를 만들지 않아 벤더 호출이 1회다

                        ### 🛠 변경
                        - 역지오코딩이 완전히 실패하면 게시글을 만들지 않는다 (500 ADDRESS_LOOKUP_FAILED)
                          - 주소 없이 저장하면 국가 코드가 비어 어느 목록에도 걸리지 않기 때문
                        - GET /api/community-posts, GET /api/community-posts/{postId} 🔓 no auth
                          (동작은 이전과 같고 문서 표기만 바로잡음)

                        ## v2.17.0 업데이트 내역

                        ### ⚠️ 주의 (클라이언트 동시 배포 필요)
                        - GET /api/community-posts 목록이 국가별로 분리됨
                          - countryCode 필수. 없으면 400(COUNTRY_NOT_SPECIFIED)
                          - GET /api/community-posts/country 로 먼저 국가를 조회한 뒤 넣는다
                          - 위치 권한을 거부한 사용자는 기기 국가 코드를 보내면 됨
                          - countryCode 는 국가(ISO 3166-1 alpha-2)이지 언어가 아니다.
                            UI 언어를 그대로 넣으면 안 된다. ja / en 을 보내면 400 이 아니라
                            200 + 빈 목록이 내려와 오류로 보이지 않는다
                            (ja → JP 가 아니고, en 에 대응하는 국가는 없다)
                        - GET /api/community-posts 페이지네이션을 커서 방식으로 전환
                          - page 파라미터 제거, cursor 파라미터 추가
                          - 응답의 page 객체가 cursor 객체(nextCursor, hasNext)로 변경
                          - 허용 파라미터는 cursor, size, scope, sort 뿐
                          - 지원하지 않는 쿼리 파라미터를 보내면 400(INVALID_QUERY_PARAMETER)

                        ### ✨ 신규
                        - GET /api/community-posts/country 좌표 국가 조회 API 추가 🔓 no auth
                          - 파라미터: latitude, longitude / 응답: countryCode
                          - 목록 조회 전에 한 번 호출해 국가를 얻는 용도. 주소는 만들지 않아 벤더 호출이 1회
                        - GET /api/community-posts/address 좌표 주소 조회 API 추가 🔒 auth required
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
                          - 역지오코딩이 실패하면 게시글이 생성되지 않음 (500 ADDRESS_LOOKUP_FAILED)
                            주소 없이 저장하면 countryCode가 비어 어느 국가 목록에도 걸리지 않기 때문
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
                        - GET /api/community-posts, GET /api/community-posts/{postId} 🔓 no auth 로 전환
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
