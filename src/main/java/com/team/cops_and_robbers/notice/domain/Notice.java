package com.team.cops_and_robbers.notice.domain;

import com.team.cops_and_robbers.common.BaseTimeEntity;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeCreateCommand;
import com.team.cops_and_robbers.notice.application.dto.command.NoticeUpdateCommand;
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

/** 공지의 분류·고정 여부만 갖는다. 제목과 내용은 언어별로 {@link NoticeTranslation} 에 있다. */
@Entity
@Table(name = "notices")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private boolean pinned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NoticeCategory category;

    /** 이 공지가 처음 작성된 언어. 요청 언어의 번역이 없을 때 대체 기준이 된다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private NoticeLanguage originalLanguage;

    public static Notice createNotice(NoticeCreateCommand command) {
        return Notice.builder()
                .pinned(command.pinned())
                .category(command.category())
                .originalLanguage(command.originalLanguage())
                .build();
    }

    public void updateNotice(NoticeUpdateCommand command) {
        this.pinned = command.pinned();
        this.originalLanguage = command.originalLanguage();
        if (command.category() != null) {
            this.category = command.category();
        }
    }
}
