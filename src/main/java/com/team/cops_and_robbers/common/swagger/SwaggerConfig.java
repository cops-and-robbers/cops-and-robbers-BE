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
                .version("2.40.0")
                .description("""
                        ## v2.40.0 업데이트 내역

                        ### 🐛 수정 — 회원 탈퇴 시 게임 기록에서 탈퇴자 정보 파기
                        - 탈퇴하면 게임 기록의 참가자 행에서 탈퇴자를 가리키는 값을 비운다. 행과 수치는 그대로 남는다
                        - GET /api/game-results/{gameResultId} 의 policeMvp, robberMvp 가 탈퇴한 사용자면 nickname 이 "알수없음"
                        - 어드민 게임 기록 참가자(AdminGameHistoryParticipant)의 userId 가 nullable (탈퇴자면 null, nickname 은 "알수없음")
                        - 어드민 게임 기록(AdminGameHistory)의 hostUserId 가 방장이 탈퇴했으면 null
                        - 이미 탈퇴한 사용자의 기록도 같은 방식으로 정리했다
                        - **앱 영향** — 응답 형식은 그대로이고, MVP 닉네임이 "알수없음"으로 올 수 있다
                        
                        """);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme)
                        .addSchemas("ErrorResponse", resolvedSchema.schema)
                );    }
}
