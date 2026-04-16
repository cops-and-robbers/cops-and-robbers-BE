package com.team.cops_and_robbers.user.presentation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AgreementRequest(
        @Schema(description = "이용약관 동의 여부", example = "true")
        @NotNull(message = "이용약관 동의 여부는 필수 입력 항목입니다.")
        Boolean termsOfService,
        @Schema(description = "개인정보처리방침 동의 여부", example = "true")
        @NotNull(message = "개인정보처리방침 동의 여부는 필수 입력 항목입니다.")
        Boolean privacyPolicy,
        @Schema(description = "위치정보 이용약관 동의 여부", example = "true")
        @NotNull(message = "위치정보 이용약관 동의 여부는 필수 입력 항목입니다.")
        Boolean locationTerms,
        @Schema(description = "마케팅 수신 동의 여부", example = "true")
        @NotNull(message = "마케팅 수신 동의 여부는 필수 입력 항목입니다.")
        Boolean marketing
) {
}
