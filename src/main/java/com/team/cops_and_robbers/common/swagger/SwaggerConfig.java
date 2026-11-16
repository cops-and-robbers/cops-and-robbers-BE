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
                .version("4.1.0")
                .description("""
                        ## v4.1.0 업데이트 내역

                        ### ✨ 추가 — 어드민 게임 구역 동네 이름
                        - 어드민 GraphQL 의 GameArea 에 placeName(String, nullable) 추가. 플레이 구역의 시도·시군구·읍면동이다 (예: 서울특별시 광진구 화양동)
                          - AdminGame.area, AdminGameHistory.area 양쪽에 내려간다
                        - AdminGameSummary 에 placeName(String, nullable) 추가. 게임 목록에서 바로 동네를 볼 수 있다
                        - 기준 지점은 원형 구역이면 중심, 다각형 구역이면 무게중심이다
                        - 커뮤니티 주소 조회를 그대로 쓰고, 찾은 이름은 좌표(약 100m 단위)별로 저장해 같은 곳은 다시 조회하지 않는다
                          - placeName 필드 하나를 조회할 때 새로 찾는 곳은 최대 5곳이고, 3초 안에 못 찾으면 이번 응답은 null 이다. 나머지는 다음 조회에서 채워진다
                          - 주소가 없거나 조회에 실패하면 null 이고, 실패한 곳은 10분 동안 다시 조회하지 않는다. 어드민 조회는 실패하지 않는다
                        - GameArea, AdminGameSummary 에 countryCode(String, nullable) 추가. ISO 3166-1 alpha-2 대문자 국가 코드다 (예: KR, JP)
                          - 동네 이름과 같은 주소 조회 결과에서 받아 함께 저장한다. 주소 조회가 국가를 주지 않은 곳은 null 이다
                        - AdminUser 에 playCountries([PlayCountry!]!) 추가. 유저가 끝까지 마친 게임을 플레이 구역의 국가로 묶어 센다
                          - PlayCountry 는 countryCode(String, nullable), games(Int!), lastPlayedAt(String, KST ISO)
                          - 많이 한 국가가 앞이고, 판 수가 같으면 최근에 한 국가가 앞이다
                          - 국가를 아직 찾지 못한 판은 countryCode 가 null 인 줄 하나로 묶어 맨 뒤에 둔다. 좌표가 없는 옛 기록은 세지 않는다
                          - 이벤트 게임에서 다시 들어와 같은 판에 기록이 여러 개여도 한 판으로 센다
                        - **앱 영향** — 없음. 어드민 GraphQL 에만 추가했다

                        ## v4.0.0 업데이트 내역

                        ### ✨ 추가 — 술래잡기 모드
                        - 게임 모드 gameMode(COPS_AND_ROBBERS | TAG) 추가
                          - 술래잡기는 감옥과 탈옥이 없고, 나머지 규칙은 경찰과도둑과 같다
                          - 요청: POST /api/games 에 gameMode(optional). 없으면 COPS_AND_ROBBERS
                          - 응답
                            - POST /api/games, GET /api/games/{gameId}, POST /api/games/join
                            - PUT /api/games/{gameId}/settings, GET /api/games/{gameId}/state
                            - 게임 결과, 로비 SETTINGS_UPDATED 이벤트
                            - 어드민 GraphQL 의 AdminGameSummary, AdminGame, AdminGameHistory, UserGameHistory
                          - 어드민 GraphQL 필터: adminGames, adminGameHistories 에 gameMode(optional) 추가
             
                        - 대기실 모드 변경 PUT /api/games/{gameId}/mode 추가
                          - 방장만 호출할 수 있고, 모드와 구역을 함께 보낸다
                          - 로비 이벤트 MODE_UPDATED(gameMode, areaData) 발행
                          - ⚠️ 에러코드 추가 — EVENT_GAME_NOT_SUPPORTED_IN_MODE(400)
                            - 이벤트 게임을 술래잡기로 바꿀 때
                            - 어드민에서 술래잡기 게임을 이벤트로 전환할 때
                            
                        - 구역의 감옥(jail) 필드가 선택 값으로 바뀜
                          - 술래잡기는 감옥을 보내지 않고, 응답의 감옥 필드는 null 이다
                          - CIRCLE 의 감옥 중심과 반경은 둘 다 보내거나 둘 다 빼야 한다
                          - ⚠️ 에러코드 추가 — JAIL_AREA_REQUIRED, JAIL_AREA_NOT_ALLOWED
                            - 경찰과도둑에 감옥이 없거나, 술래잡기에 감옥이 있을 때
                            - POST /api/games, PUT /api/games/{gameId}/area, PUT /api/games/{gameId}/mode
                            
                        - 술래잡기에서 탈옥 POST /api/games/{gameId}/system/escape 거절
                          - ⚠️ 에러코드 추가 — ESCAPE_NOT_ALLOWED_IN_MODE(400)
                          
                        - FCM 시스템 알림 문구가 모드별로 바뀜
                          - 술래잡기는 "술래", "도망자", "잡힘" 문구를 쓴다
                        - 식별자는 그대로다
                          - POLICE 는 술래, ROBBER 는 도망자, JAILED 는 잡힌 상태로 해석한다

                        ### 🧹 삭제 — Deprecated API
                        - 로비 준비 상태 변경 PATCH /api/games/{gameId}/lobby/ready
                        - 참가자 isReady
                          - 로비 조회 GET /api/games/{gameId}/lobby
                          - 로비 이벤트 ENTER, TEAM_UPDATE, HOST_CHANGED
                        - 구역 평면 필드 (요청, 응답 모두)
                          - playgroundCenter, playgroundRadiusInMeters, jailCenter, jailRadiusInMeters
                          - circle / polygon 을 쓴다. 요청에 areaType 이 필수가 된다
                        - 도둑 위치 조회 GET /api/games/{gameId}/robbers/location
                        - 구버전 소켓 경로 /game-connection
                        - 앱 영향
                          - 이 API 를 쓰는 구버전 앱은 동작하지 않는다
                          - 배포 전에 강제 업데이트 최소 버전을 올려야 한다

                        ## v3.3.0 업데이트 내역

                        ### ✨ 추가 — 웹 회원 탈퇴 API
                        - POST /api/auth/withdrawal/preview 추가. Firebase ID 토큰으로 계정을 확인하고 닉네임과 게임 참여 여부를 응답한다.
                        - POST /api/auth/withdrawal 추가. DELETE /api/user/me 와 같은 탈퇴 처리를 한다.
                        - 두 API 모두 회원가입을 하지 않고 JWT 도 발급하지 않는다.
                        - ⚠️ 에러코드 추가 — WITHDRAWAL_USER_NOT_FOUND. 가입되지 않은 계정이면 404 를 응답한다.
                        - 앱 영향 — 없음. 웹사이트의 계정 삭제 페이지에서만 사용한다.

                        ## v3.2.0 업데이트 내역

                        ### ✨ 추가 — 경찰 무전 접속 토큰 API
                        - 무전 접속 토큰 발급 POST /api/games/{gameId}/radio/token 추가. 경찰이 무전 서버에 들어갈 토큰을 받는다
                          - 요청 본문 없음(로그인한 경찰 본인). 응답은 url, room, identity, token, expiresAt
                          - 토큰 수명은 2분이다. 앱은 수명의 70~90% 지점에서 랜덤하게 다시 호출해 재발급받는다
                          - 재발급 때마다 자격을 다시 검증한다. 게임이 끝났거나 나갔으면 거절된다
                          - 음성은 서버를 거치지 않고 무전 서버와 앱 사이에서만 흐른다. 앱은 url 을 하드코딩하지 않는다
                          - ⚠️ 에러코드 추가 — ONLY_POLICE_CAN_USE_RADIO(403), RADIO_POLICE_WAITING_TIME(400), RADIO_NOT_ALLOWED_IN_EVENT_GAME(400)
                        - 무전 사용 시간을 라운드별 참가자 기록에 누적한다. 어드민 GraphQL 의 AdminGameHistoryParticipant 에 radioSeconds(Int!), AdminUser 에 누적 radioSeconds(Int!) 추가

                        ### 변경 — 최대 참가 인원 150명 → 50명
                        - ⚠️ maxParticipants 상한이 50 으로 내려간다. 51 이상이면 400 이다
                          - POST /api/games 의 settings, PUT /api/games/{gameId}/settings
                        - 앱 영향 — 방 생성 인원 슬라이더 최대값을 50 으로 맞춘다
                        """);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme)
                        .addSchemas("ErrorResponse", resolvedSchema.schema)
                );    }
}
