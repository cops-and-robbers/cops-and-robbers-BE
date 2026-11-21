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
                .version("4.4.0")
                .description("""
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
