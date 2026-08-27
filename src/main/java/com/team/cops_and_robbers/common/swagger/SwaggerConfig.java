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
                .version("2.26.0")
                .description("""
                        ## v2.26.0 업데이트 내역

                        ### ✨ 신규 — 커뮤니티 알림함
                        - 알림은 댓글이 달린 시점에 받을 사람을 확정해 저장한다
                          - 조회 시점에 다시 계산하지 않으므로 알림을 켜기 전 댓글이 소급되지 않는다
                          - 제목·내용은 그 시점 값을 복사해 두어 원본 댓글이 지워져도 알림함에 남는다
                        - GET /api/community-posts/notifications — 알림 목록 (최신순 커서, 최근 60일)
                        - GET /api/community-posts/notifications/unread-count — 배지용 안 읽은 개수
                        - POST /api/community-posts/notifications/read — 읽음 처리
                          - 알림마다 읽음 플래그를 두지 않고 유저당 읽음 커서 하나로 판정한다
                          - 목록 조회(GET)는 읽음 처리하지 않는다. 이 API를 따로 호출해야 한다

                        ### ✨ 신규 — 게시글별 알림 켜기/끄기
                        - PUT /api/community-posts/{postId}/notification-settings — 설정 변경 (notifyComments, notifyReplies)
                        - 현재 설정은 게시글 단건 조회 응답의 notificationSettings로 내려간다 (별도 조회 API 없음)
                          - 목록·비로그인 조회는 null
                        - 토글을 건드린 글만 설정이 저장되고, 건드리지 않은 글은 기본값을 따른다
                          - 내가 쓴 글 → 댓글 ON / 답글 OFF
                          - 그 외 → 둘 다 OFF (켠 사람만 받는다)
                        - 좋아요·스크랩은 알림을 보내지 않는다

                        ### ✨ 신규 — 댓글별 답글 알림 켜기/끄기
                        - PUT /api/community-posts/comments/{commentId}/notification — 설정 변경 (notifyReplies, 기본 true)
                          - 작성자만 바꿀 수 있다. 남의 댓글이면 403(FORBIDDEN_NOT_COMMENT_AUTHOR)
                        - 현재 상태는 댓글 목록 응답의 notifyReplies로 내려간다
                        - "내 댓글에 달린 답글" 알림은 게시글 설정과 독립이다
                          - 그 글의 알림을 꺼도 내 댓글의 답글 알림은 이 값만 따른다
                          - 남의 글에 댓글을 여러 개 썼다면 시끄러운 댓글 하나만 끌 수 있다

                        ### ✨ 신규 — 커뮤니티 푸시
                        - GET /api/user/agreements/community-push — 수신 동의 조회
                        - PUT /api/user/agreements/community-push — 수신 동의 변경 (allowCommunityPush, 기본 true)
                        - 푸시를 꺼도 알림함에는 그대로 쌓인다. 푸시와 기록은 별개다
                        - GET /api/user/me 응답에 allowCommunityPush 추가

                        ### ✨ 신규 — 채팅 알림
                        - 채팅 메시지가 오면 방 참여자에게 FCM 푸시를 보낸다 (보낸 사람 제외)
                          - 입장·퇴장 안내(SYSTEM)는 보내지 않는다
                          - 게임 초대(GAME_INVITE)는 본문이 JSON이라 "게임에 초대했습니다" 고정 문구로 보낸다
                        - PUT /api/community-posts/{postId}/chat/notification — 방별 알림 켜기/끄기 (기본 켜짐)
                          - 끄면 푸시만 막고 메시지 저장과 안 읽은 개수는 그대로다
                        - POST /api/community-posts/{postId}/chat/read — 방별 읽음 처리
                          - 채팅 내역 조회(GET)는 읽음 처리하지 않는다. 읽은 위치는 앞으로만 이동한다
                        - 채팅방 목록 응답에 unreadCount, notificationEnabled 추가
                          - unreadCount는 내가 보낸 메시지와 입장·퇴장 안내를 세지 않는다
                        - 커뮤니티 푸시 수신 동의(allowCommunityPush)는 댓글과 채팅에 모두 적용된다
                        - 지금 그 방을 보고 있는 사람도 푸시를 받는다. 앱이 포그라운드에서 현재 방이면 표시하지 않는 쪽으로 처리한다
                        - 댓글·답글 푸시는 제목이 게시글 제목, 본문이 댓글 내용이다. 댓글/답글 구분은 data.type으로 한다

                        ### 🛠 변경
                        - 게시글을 삭제하면 그 글의 알림과 알림 설정도 함께 지워진다

                        ## v2.25.0 업데이트 내역

                        ### ✨ 신규 — 커뮤니티 채팅 멤버 관리
                        - GET /api/community-posts/{postId}/chat/members — 채팅방 멤버 목록 조회
                          - userId, nickname, profileIcon, isAuthor(작성자=방장 여부)
                          - 해당 방 멤버만 조회 가능, 아니면 403(NOT_A_CHAT_MEMBER)
                        - DELETE /api/community-posts/{postId}/chat/members/{userId} — 채팅방 멤버 강퇴
                          - 방장만 가능. 자기 자신 강퇴 시 400(CANNOT_KICK_SELF), 대상이 멤버 아니면 404(CHAT_MEMBER_NOT_FOUND), 방장 아니면 403(FORBIDDEN_NOT_CHAT_HOST)
                          - 강퇴된 유저는 재입장 제한 없음
                          - 서버는 웹소켓 세션을 강제로 끊지 않는다. KICK 시스템 메시지를 브로드캐스트하며, 앱이 자기 자신 대상 메시지를 받으면 스스로 구독을 해제해야 한다
                        - 게시글 응답(목록·단건·생성·수정·내 스크랩 목록)에 chatJoined(boolean) 추가 — 내가 그 게시글 채팅방에 참여 중인지
                          - 단건 조회는 여전히 로그인 없이 호출 가능하며, 비로그인 조회는 chatJoined가 항상 false
                        - 채팅 메시지 응답(내역 조회·목록의 lastMessage)에 발신자 프로필 아이콘 senderProfileIcon 추가
                          - lastMessage에 senderNickname도 함께 추가
                          - 닉네임과 같은 규칙: 조회 시점 현재 값을 우선하고, 탈퇴 시 발신 시점 값으로 대체
                        ## v2.24.0 업데이트 내역

                        ### ✨ 신규 — 프로필 아이콘
                        - 프로필 아이콘은 앱 내장 SVG 에셋이라 이미지가 아닌 아이콘 번호(정수)만 저장한다
                        - 회원 정보에 profileIcon 필드 추가 (기본값 1)
                          - GET /api/user/me 응답에 포함
                          - PATCH /api/user/me/profile-icon — 아이콘 번호 저장·수정 (요청: profileIcon)
                        - 서버는 번호 범위를 검증하지 않는다. 앱 에셋과 1:1 대응하며 현재 1, 2가 있고 이후 앱에서만 늘어난다
                        - 커뮤니티 모집글 목록·단건, 댓글 응답에 작성자 writerProfileIcon 추가
                          - 탈퇴한 작성자는 기본 아이콘 번호로 내려온다 (닉네임이 "알수없음"으로 오는 경우와 동일 시점)
                          - 삭제된 댓글은 writerId·writerNickname처럼 writerProfileIcon도 null

                        ### 🛠 변경 — 인기순(POPULAR) 정렬 지원
                        - 지금까지 sort=POPULAR 요청은 400(UNSUPPORTED_LIST_SORT)으로 막혀 있었는데, 규칙을 정하고 구현했다
                        - 점수 = 좋아요*1 + 스크랩*2 + 채팅 참여(멤버 수)*3
                        - 최근 7일 이내 작성된 글만 대상이며, 마감된 글은 다른 정렬과 동일하게 항상 맨 뒤

                        ## v2.23.0 업데이트 내역

                        ### ✨ 신규 — 커뮤니티 신고 (모집글 · 채팅)
                        - POST /api/report/community-post — 모집글 신고
                          - postId, reportType, etcReason
                        - POST /api/report/community-chat — 채팅 메시지 신고
                          - chatMessageId, reportType, etcReason
                          - chatMessageId는 채팅 내역 조회 응답의 id를 그대로 쓴다
                        - reportType은 인게임 채팅 신고와 같은 값을 쓴다 (신규 값 없음)
                          - FISHING / VERBAL_ABUSE / IMPERSONATION / SPAM / CHEATING / DEMORALIZATION / ETC
                        - ETC면 etcReason 필수, 최대 300자. 없으면 400(ETC_REASON_REQUIRED)
                        - 본인을 신고하면 400(SELF_REPORT)
                        - 같은 대상을 다시 신고하면 409(DUPLICATE_REPORT)
                          - 모집글은 신고자+작성자+글, 채팅은 신고자+발신자+메시지 기준
                        - 없는 글은 404(POST_NOT_FOUND), 없는 메시지는 404(CHAT_MESSAGE_NOT_FOUND)

                        ### 🛠 변경
                        - 게시글 응답의 writerNickname이 탈퇴한 작성자면 "알수없음" (기존 null)
                          - 목록 · 상세 · 생성 · 수정 응답에 모두 적용된다
                        - 작성자가 탈퇴하면 그 사람의 모집중(RECRUITING) 게시글이 COMPLETED로 바뀐다
                          - 응답할 사람이 없는 모임에 참여 요청이 계속 가지 않도록 하기 위함
                          - 이미 마감했거나 날짜가 지난 글은 건드리지 않는다

                        ## v2.22.0 업데이트 내역

                        ### ✨ 신규 — 댓글·좋아요·스크랩
                        - POST /api/community-posts/{postId}/comments — 댓글/답글 작성
                          - parentId를 보내면 답글, 생략하면 일반 댓글
                          - 댓글은 2depth로 고정. 답글에 답글을 달면 400(INVALID_COMMENT_DEPTH)
                        - GET /api/community-posts/{postId}/comments — 댓글 목록 조회 🔓 no auth
                          - 오래된 순 커서 페이지네이션. 답글은 부모 댓글의 replies에 함께 담겨 내려온다
                          - 답글이 남아있는 삭제된 댓글은 deleted: true로 남고 content·writerNickname은 null
                          - 탈퇴한 작성자의 댓글은 writerNickname이 "알수없음" (writerId는 그대로 내려간다)
                        - DELETE /api/community-posts/comments/{commentId} — 댓글 삭제
                          - 작성자만 삭제 가능
                          - 답글이 남아있으면 실제로 지우지 않고 삭제 표시만 한다 (마지막 답글이 지워질 때 함께 정리됨)
                        - POST/DELETE /api/community-posts/{postId}/likes — 좋아요 · 취소
                          - 중복 좋아요는 409(ALREADY_LIKED), 누른 적 없는 취소는 404(LIKE_NOT_FOUND)
                        - POST/DELETE /api/community-posts/{postId}/scraps — 스크랩 · 취소
                          - 중복 스크랩은 409(ALREADY_SCRAPPED), 스크랩한 적 없는 취소는 404(SCRAP_NOT_FOUND)
                        - GET /api/community-posts/scraps — 내 스크랩 목록 조회
                          - 스크랩한 순서(최신순) 커서 페이지네이션

                        ## v2.21.0 업데이트 내역

                        ### ✨ 신규 — 커뮤니티 목록 정렬 · 검색
                        - GET /api/community-posts sort=DEADLINE 지원 (모임 날짜 오름차순)
                        - GET /api/community-posts sort=DISTANCE 지원 (사용자 좌표 기준 가까운 순)
                          - latitude, longitude 필수. 없으면 400
                          - 다른 정렬에서 좌표를 보내면 400 (조용히 무시되지 않도록)
                          - POPULAR는 아직 400
                        - GET /api/community-posts keyword 검색 추가
                          - 제목 · 장소명(placeName) · 지역(region)에서 찾는다 (대소문자 무시)
                          - 화면에 보이는 글자로 찾을 수 있게 셋을 함께 본다. 지번 주소는 복사용이라 제외
                          - 공백 제외 2자 이상. 미만이면 400(INVALID_QUERY_PARAMETER), 공백만 보내면 전체 조회
                        - 커서에 국가·정렬·검색어가 담긴다. 하나라도 요청과 다르면 400
                          - 거리 기준 좌표는 담지 않는다. 이동해도 커서가 막히지 않게 하기 위함

                        ### 🛠 변경
                        - 마감된 게시글은 정렬과 무관하게 목록 맨 뒤로
                          - 작성자가 마감(COMPLETED)했거나 모임 날짜가 지난(ENDED) 글
                        - 모임 날짜가 지난 게시글의 status를 ENDED로 내려줌
                          - RECRUITING / COMPLETED / ENDED (ENDED 신규)
                          - 저장하지 않고 조회 시점에 판정한다
                        - 게시글 좌표를 PostGIS Point로 저장 (응답의 latitude/longitude는 그대로)

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
