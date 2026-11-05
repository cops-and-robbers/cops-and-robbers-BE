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
                .version("3.0.0")
                .description("""
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
