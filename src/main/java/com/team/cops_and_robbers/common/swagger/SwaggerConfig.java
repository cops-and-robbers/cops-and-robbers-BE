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
                .version("2.7.0")
                .description("""
                        ## v2.7.0 업데이트 내역

                        ### ✅ 신규 API
                        - 게임 푸시 알림 수신 동의 여부 조회 API 추가 (GET /api/user/agreements/game-push)
                        - 게임 푸시 알림 수신 동의 여부 업데이트 API 추가 (PUT /api/user/agreements/game-push)

                        ### 🛠 변경 및 수정
                        - 이전 API에 운동장 -> 플레이그라운드로 스웨거 명세 수정
                        - 게임 알림에 이모지 제거
                        - timezone suffix 누락 버그 수정: 아래 필드들이 +09:00 포함된 형식으로 변경됨
                          - GET /api/games/{gameId} → gameStartTime
                          - POST /api/games → createdAt
                          - GET /api/notices, GET /api/notices/{noticeId} → createdAt, updatedAt

                        ## v2.6.0 업데이트 내역

                        ### ✅ 신규 API

                        **게임 상태 조회** `GET /api/games/{gameId}/state`
                        - 앱 재접속·소켓 재연결 시 게임 화면 복원에 사용 (기존 도둑 위치 조회 API를 대체)
                        - 응답: 마지막 위치 공개 주기의 생존 도둑 위치 스냅샷 + 전체 참여자 팀·상태
                        - `robberLocations` 반환 조건
                          - 경찰 대기 시간 중 (POLICE_WAITING): 도둑 위치 빈 배열 — 경찰·도둑 모두 위치 비공개
                          - 경찰 출동 후 첫 공개 주기 전: 도둑 위치 빈 배열
                          - 첫 도둑 위치 공개 이후: 생존 도둑 위치 목록 (JAILED 도둑 제외)
                        - 자세한 내용은 해당 API 항목을 확인하세요.

                        ### ⚠️ Deprecated API

                        **도둑 위치 조회** `GET /api/games/{gameId}/robbers/location`
                        - 기존 강제 종료 후 재접속 시 위치 공개 버그는 해결된 상태입니다.
                        - `state` API의 `robberLocations` 필드로 동일한 데이터를 제공받을 수 있습니다 (위 `state` API로 마이그레이션 필요)
                        - 현재는 호출 가능하나, 프론트 마이그레이션 완료 후 삭제됩니다
                        """);

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes("JWT", securityScheme)
                        .addSchemas("ErrorResponse", resolvedSchema.schema)
                );    }
}
