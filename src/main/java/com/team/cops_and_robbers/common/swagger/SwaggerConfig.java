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
                .version("2.15.0")
                .description("""
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
