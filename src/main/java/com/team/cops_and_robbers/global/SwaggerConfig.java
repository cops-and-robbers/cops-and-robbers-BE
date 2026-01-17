package com.team.cops_and_robbers.global;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


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

        Info info = new Info().title("👮 경찰과 도둑 API 🥷").version("1.0.0");

        List<Server> servers = List.of(
                new Server().url("http://localhost:8080").description("로컬 개발 서버")
        );

        return new OpenAPI()
                .info(info)
                .servers(servers)
                .addSecurityItem(securityRequirement)
                .components(new Components().addSecuritySchemes("JWT", securityScheme));
    }
}
