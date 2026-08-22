package com.team.cops_and_robbers.community.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.community.application.dto.CommunityPostCursor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("커뮤니티 게시글 커서")
class CommunityPostCursorTest {

    @Test
    void 인코딩한_커서를_다시_디코딩하면_같은_값이_나온다() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 15, 12, 30, 45, 123456000);

        String encoded = CommunityPostCursor.encode(true, createdAt, 42L);
        CommunityPostCursor decoded = CommunityPostCursor.decode(encoded).orElseThrow();

        assertThat(decoded.closed()).isEqualTo(1);
        assertThat(decoded.createdAt()).isEqualTo(createdAt);
        assertThat(decoded.id()).isEqualTo(42L);
    }

    @Test
    void null이나_빈_문자열은_커서_없음으로_디코딩된다() {
        assertThat(CommunityPostCursor.decode(null)).isEmpty();
        assertThat(CommunityPostCursor.decode("")).isEmpty();
        assertThat(CommunityPostCursor.decode("  ")).isEmpty();
    }

    @Test
    void base64가_아닌_커서는_INVALID_QUERY_PARAMETER_예외가_발생한다() {
        assertThatThrownBy(() -> CommunityPostCursor.decode("not-base64!!!"))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(CommonException.INVALID_QUERY_PARAMETER.getDetail());
    }

    @Test
    void 구분자가_없는_커서는_INVALID_QUERY_PARAMETER_예외가_발생한다() {
        String noDelimiter = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("hello".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> CommunityPostCursor.decode(noDelimiter))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(CommonException.INVALID_QUERY_PARAMETER.getDetail());
    }

    @Test
    void 시각_형식이_잘못된_커서는_INVALID_QUERY_PARAMETER_예외가_발생한다() {
        String invalidTime = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("0|어제|42".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> CommunityPostCursor.decode(invalidTime))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(CommonException.INVALID_QUERY_PARAMETER.getDetail());
    }
}
