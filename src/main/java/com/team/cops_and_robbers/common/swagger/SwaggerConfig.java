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
                .version("2.41.0")
                .description("""
                        ## v2.41.0 업데이트 내역

                        ### ✨ 추가 — 어드민 유저 상세에 끝난 게임 이력
                        - 어드민 유저(AdminUser)에 gameHistories(limit: Int = 20) 추가. 끝난 게임을 최신순으로 내려준다 (limit 최대 100)
                        - 방에서 나가도 남는 게임 결과 기록에서 읽는다. 기존 participations 는 지금 들어가 있는 방 그대로
                        - 이벤트 게임에서 나갔다 다시 들어와 같은 판에 기록이 여러 개면 가장 최근 기록 하나만 내려준다
                        - **앱 영향** — 없음. 어드민 GraphQL 에만 추가했다
                        
                        """);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme)
                        .addSchemas("ErrorResponse", resolvedSchema.schema)
                );    }
}
