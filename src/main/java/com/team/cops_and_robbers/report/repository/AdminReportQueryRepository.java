package com.team.cops_and_robbers.report.repository;

import com.team.cops_and_robbers.report.domain.ChatReport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdminReportQueryRepository extends Repository<ChatReport, Long> {

    String GAME_CHAT_REPORTS = """
            select id, 'GAME_CHAT' as source, reporter_user_id, reported_user_id, status, created_at,
                   message_content as content
            from reports
            """;

    String COMMUNITY_POST_REPORTS = """
            select id, 'COMMUNITY_POST' as source, reporter_user_id, reported_user_id, status, created_at,
                   post_title as content
            from community_post_reports
            """;

    String COMMUNITY_CHAT_REPORTS = """
            select id, 'COMMUNITY_CHAT' as source, reporter_user_id, reported_user_id, status, created_at,
                   message_content as content
            from community_chat_reports
            """;

    String UNIFIED_REPORTS = """
            select id, source, reporter_user_id, reported_user_id, status, created_at, content
            from (
            """
            + GAME_CHAT_REPORTS + " union all " + COMMUNITY_POST_REPORTS + " union all " + COMMUNITY_CHAT_REPORTS
            + """
            ) unified
            where (:status is null or status = :status)
              and (:source is null or source = :source)
            """;

    @Query(value = UNIFIED_REPORTS + " order by created_at desc limit :size offset :offset", nativeQuery = true)
    List<AdminAllReportRow> findAllDesc(
            @Param("status") String status,
            @Param("source") String source,
            @Param("size") int size,
            @Param("offset") long offset
    );

    @Query(value = UNIFIED_REPORTS + " order by created_at asc limit :size offset :offset", nativeQuery = true)
    List<AdminAllReportRow> findAllAsc(
            @Param("status") String status,
            @Param("source") String source,
            @Param("size") int size,
            @Param("offset") long offset
    );

    @Query(value = "select count(*) from (" + UNIFIED_REPORTS + ") counted", nativeQuery = true)
    long countAll(@Param("status") String status, @Param("source") String source);
}
