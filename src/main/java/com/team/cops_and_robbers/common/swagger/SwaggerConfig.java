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
                .version("3.1.0")
                .description("""
                        ## v3.1.0 업데이트 내역

                        ### ✨ 추가 — 구역 축소 최종 반경, 강제 체포 API
                        - 게임 설정에 shrinkFinalRadiusInMeters(Integer, nullable) 추가. null 이면 축소 꺼짐
                          - 요청: POST /api/games 의 settings, PUT /api/games/{gameId}/settings
                          - 응답: POST /api/games, GET /api/games/{gameId}, PUT /api/games/{gameId}/settings, 로비 SETTINGS_UPDATED 이벤트
                        - 값이 있을 때만 검증한다. 구역 타입이 CIRCLE 이고, 감옥 반경보다 크고 플레이그라운드 반경보다 작아야 한다
                        - ⚠️ 에러코드 추가 — SHRINK_REQUIRES_CIRCLE_AREA, INVALID_SHRINK_FINAL_RADIUS
                          - POST /api/games, PUT /api/games/{gameId}/settings, PUT /api/games/{gameId}/area 세 곳에서 발생한다
                          - PUT /api/games/{gameId}/settings 는 구역을 함께 조회하므로 기존 GAME_AREA_NOT_FOUND(404) 도 새로 발생할 수 있다
                          - 구역을 바꿀 때 저장된 최종 반경이 조건에 안 맞으면 PUT /area 가 400 이다. 앱이 축소를 먼저 끄고 구역을 저장하면 된다
                        - 강제 체포 POST /api/games/{gameId}/system/force-arrest 추가. 구역 밖에 오래 머문 도둑이 스스로 체포된다
                          - 요청 본문 없음(로그인한 도둑 본인), 응답은 /system/arrest 와 같다
                          - 경찰 대기 시간 중에도 호출할 수 있고, 경찰의 체포 수는 올라가지 않는다
                          - ⚠️ 에러코드 추가 — FORCE_ARREST_NOT_ALLOWED_IN_EVENT_GAME. 이벤트 게임에서 호출하면 400 이다
                        - ⚠️ ARREST 이벤트의 police 가 null 가능으로 전환 — 강제 체포일 때 null 이다
                          - 앱은 police 가 null 이면 "구역 밖에 오래 있어서 잡혔다"로 표시한다
                        - 게임이 끝나도 남도록 GameResult 에 최종 반경을 함께 복사한다. 어드민 GraphQL 의 AdminGame, GameResult, AdminGameHistory 에 shrinkFinalRadiusInMeters(Int, nullable) 추가
                        - 앱 영향 — 최종 반경 계산, 축소 표시, 구역 이탈 판정과 경고 횟수는 앱이 처리한다. 서버는 위치를 검증하지 않는다

                        ## v3.0.0 업데이트 내역

                        ### 변경 — 준비(Ready) 상태 제거, 방장이 시작하면 바로 게임 시작
                        - POST /api/games/{gameId}/lobby/start 는 방장 권한과 팀 구성만 확인, 준비 여부는 보지 않는다
                        - 에러코드 NOT_ALL_READY, HOST_CANNOT_UNREADY 삭제
                        - 팀을 바꾸거나 라운드가 끝나도 풀릴 준비 상태가 없다
                        - 로비 이벤트 READY_UPDATE 는 더 이상 발행하지 않는다
                        - 앱 영향
                          1. 구버전 앱은 그대로 동작한다(아래 Deprecated 항목 유지)
                          2. 신버전은 준비 UI 와 시작 버튼의 준비 조건을 빼면 된다

                        ### Deprecated API

                        로비 준비 상태 변경 `PATCH /api/games/{gameId}/lobby/ready`
                        - 호출해도 아무것도 바뀌지 않고 이벤트도 없다. 응답 코드는 예전과 같고, 방장이 호출했을 때만 400 대신 204
                        - 구버전 앱 호환용으로만 남겨 두고, 다음 메이저에서 삭제 예정

                        참가자 isReady
                          - 로비 조회 API(GET /api/games/{gameId}/lobby), 로비 이벤트(ENTER, TEAM_UPDATE, HOST_CHANGED)에서 항상 true 반환
                        """);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme)
                        .addSchemas("ErrorResponse", resolvedSchema.schema)
                );    }
}
