package com.team.cops_and_robbers.play.radio.presentation;

import com.team.cops_and_robbers.common.ControllerTest;
import com.team.cops_and_robbers.play.radio.repository.RadioOverloadRepository;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class InternalRadioControllerTest extends ControllerTest {

    private static final String OVERLOAD_URL = "/internal/radio/overload";
    private static final String API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String API_KEY = "test-internal-api-key";

    @Autowired
    private RadioOverloadRepository radioOverloadRepository;

    @AfterEach
    void tearDown() {
        radioOverloadRepository.deactivate();
    }

    @Nested
    @DisplayName("무전 서버 과부하 신호 API")
    class UpdateOverload {

        @Test
        void 과부하_신호를_받으면_차단을_켠다() {
            // when
            ExtractableResponse<Response> response = sendOverload(API_KEY, "firing");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
                softly.assertThat(radioOverloadRepository.isActive()).isTrue();
            });
        }

        @Test
        void 해제_신호를_받으면_차단을_끈다() {
            // given
            sendOverload(API_KEY, "firing");

            // when
            ExtractableResponse<Response> response = sendOverload(API_KEY, "resolved");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
                softly.assertThat(radioOverloadRepository.isActive()).isFalse();
            });
        }

        @Test
        void 내부_API_키가_틀리면_401을_반환한다() {
            // when
            ExtractableResponse<Response> response = sendOverload("wrong-key", "firing");

            // then
            assertSoftly(softly -> {
                softly.assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
                softly.assertThat(radioOverloadRepository.isActive()).isFalse();
            });
        }

        @Test
        void 내부_API_키가_없으면_401을_반환한다() {
            // when
            ExtractableResponse<Response> response = unauthenticated()
                    .body(Map.of("status", "firing"))
                    .when()
                    .post(OVERLOAD_URL)
                    .then()
                    .extract();

            // then
            assertSoftly(softly -> softly.assertThat(response.statusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED.value()));
        }
    }

    private ExtractableResponse<Response> sendOverload(String apiKey, String status) {
        return unauthenticated()
                .header(API_KEY_HEADER, apiKey)
                .body(Map.of("status", status))
                .when()
                .post(OVERLOAD_URL)
                .then()
                .extract();
    }
}
