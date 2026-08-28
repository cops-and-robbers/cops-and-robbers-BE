package com.team.cops_and_robbers.notice.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeTranslationCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공지 본문의 언어별 번역. 원문도 한 언어의 번역으로 같은 자리에 둔다.
 * 원문만 notices 에 남기는 구조는 같은 글이 두 군데 살게 되고, 이 테이블에
 * 원문 언어 행이 들어가는 것을 막을 방법도 없어서 접었다. 대칭으로 두면
 * (notice_id, language) 유니크 제약 하나로 정합성이 지켜진다.
 */
@Entity
@Table(name = "notice_translations")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeTranslation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long noticeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private NoticeLanguage language;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public static NoticeTranslation createTranslation(Long noticeId, NoticeTranslationCommand command) {
        return NoticeTranslation.builder()
                .noticeId(noticeId)
                .language(command.language())
                .title(command.title())
                .content(command.content())
                .build();
    }
}
