package com.team.cops_and_robbers.common;

import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class CorsConfigurationTest extends ControllerTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "https://malicious.example.com";

    @Nested
    @DisplayName("/api/notices CORS")
    class NoticesCors {

        @Test
        void 허용된_오리진의_preflight_요청에_CORS_헤더를_응답한다() {
            ExtractableResponse<Response> extract = given()
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "GET")
                    .header("Access-Control-Request-Headers", "Authorization")
                    .when()
                    .options("/api/notices")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.header("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
            });
        }

        @Test
        void preflight_요청이_AuthInterceptor에_의해_막히지_않는다() {
            ExtractableResponse<Response> extract = given()
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .when()
                    .options("/api/notices")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
            });
        }

        @Test
        void 허용되지_않은_오리진의_preflight_요청은_CORS_헤더를_응답하지_않는다() {
            ExtractableResponse<Response> extract = given()
                    .header("Origin", DISALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "GET")
                    .when()
                    .options("/api/notices")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.header("Access-Control-Allow-Origin")).isNull();
            });
        }
    }

    @Nested
    @DisplayName("/graphql CORS")
    class GraphqlCors {

        @Test
        void 허용된_오리진의_preflight_요청에_CORS_헤더를_응답한다() {
            ExtractableResponse<Response> extract = given()
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .when()
                    .options("/graphql")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.header("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
            });
        }

        @Test
        void preflight_요청이_AdminInterceptor에_의해_막히지_않는다() {
            ExtractableResponse<Response> extract = given()
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .when()
                    .options("/graphql")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
            });
        }
    }

    @Nested
    @DisplayName("/api/auth CORS")
    class AuthCors {

        @Test
        void 허용된_오리진의_preflight_요청에_CORS_헤더를_응답한다() {
            ExtractableResponse<Response> extract = given()
                    .header("Origin", ALLOWED_ORIGIN)
                    .header("Access-Control-Request-Method", "POST")
                    .when()
                    .options("/api/auth/login")
                    .then()
                    .extract();

            assertSoftly(softly -> {
                softly.assertThat(extract.statusCode()).isEqualTo(200);
                softly.assertThat(extract.header("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
            });
        }
    }
}