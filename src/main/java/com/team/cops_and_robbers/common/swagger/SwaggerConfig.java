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
                .version("4.5.0")
                .description("""
                        ## v4.5.0 업데이트 내역

                        ### 🔁 변경 — 경찰·도둑 용어를 모드 공통 용어로 변경
                        - 쫓는 쪽은 CHASER, 쫓기는 쪽은 RUNNER 로 부른다. 화면 문구(경찰/술래/좀비)는 앱이 gameMode 를 보고 정한다
                        - 응답 필드에 새 이름을 추가한다. 옛 이름도 같은 값으로 그대로 내려간다 (deprecated)
                          - policeWaitMinutes → chaserWaitMinutes (게임 생성·조회·설정 변경, 로비 SETTINGS_UPDATED)
                          - policeMvp / robberMvp / remainingRobberCount → chaserMvp / runnerMvp / remainingRunnerCount (게임 결과)
                          - police / robbers → chasers / runners (참가자 목록)
                          - robberLocations → runnerLocations (게임 상태)
                          - robberNickname / remainingThieves → runnerNickname / remainingRunners (체포 응답)
                          - police / robber / remainingThieves → chaser / runner / remainingRunners (시스템 ARREST)
                          - escapedThief / remainingThieves → escapedRunner / remainingRunners (시스템 ESCAPE)
                        - 요청은 옛 이름과 새 이름을 모두 받는다
                          - policeWaitMinutes → chaserWaitMinutes (게임 생성·설정 변경)
                          - robberParticipantId → runnerParticipantId (체포)
                          - 팀 변경 targetTeam 은 POLICE / ROBBER 와 CHASER / RUNNER 를 모두 받는다
                        - 구독 경로를 추가한다. 옛 경로에도 같은 메시지가 같이 발행된다
                          - /subscribe/game/{gameId}/chat/chaser, /chat/runner
                          - /subscribe/game/{gameId}/ping/chaser, /ping/runner
                        - 응답 enum 값, 시스템 이벤트 type, FCM type, 에러 코드는 아직 옛 이름 그대로다
                          - 이후 버전에서 CHASER / RUNNER, CHASER_WAITING, CHASER_FORFEITED, CHASER_MOVE_START, RUNNER_LOCATION_REVEAL, ONLY_CHASER_CAN_ARREST 등으로 바뀐다
                        - **앱 영향** — 새 앱은 새 필드와 새 구독 경로를 쓰고, enum 값과 에러 코드는 옛 이름과 새 이름을 둘 다 처리한다. 이전 앱은 그대로 동작한다

                        ## v4.4.0 업데이트 내역

                        ### ✨ 추가 — 무전 서버 혼잡 시 신규 입장 거절
                        - 무전 토큰 발급 POST /api/games/{gameId}/radio/token 에 503 RADIO_SERVER_BUSY 추가
                          - 무전 서버 CPU가 높을 때 새로 무전에 들어오는 경찰만 거절한다
                          - 이미 무전 중인 경찰의 토큰 재발급은 그대로 성공한다
                        - 혼잡이 풀리면 같은 요청이 다시 성공한다. 별도로 상태를 조회할 필요는 없다
                        - **앱 영향** — RADIO_SERVER_BUSY 를 받으면 혼잡 문구를 띄우고 무전 버튼을 잠시 막는다. 처리하지 않은 이전 앱에서는 일반 에러로 보인다
                        """);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme)
                        .addSchemas("ErrorResponse", resolvedSchema.schema)
                );    }
}
