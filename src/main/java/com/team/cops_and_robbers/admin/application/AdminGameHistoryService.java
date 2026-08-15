package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.command.game.AdminGameHistoryListCommand;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryPageResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryParticipantResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameHistoryResult;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.repository.GameResultParticipantRepository;
import com.team.cops_and_robbers.history.repository.GameResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGameHistoryService {

    private final GameResultRepository gameResultRepository;
    private final GameResultParticipantRepository gameResultParticipantRepository;

    public AdminGameHistoryPageResult getGameHistoryList(AdminGameHistoryListCommand command) {
        Page<GameResult> resultPage = gameResultRepository.findAllForAdmin(
                command.endReason(), command.toPageable());
        Page<AdminGameHistoryResult> historyPage = resultPage.map(AdminGameHistoryResult::from);
        return AdminGameHistoryPageResult.from(historyPage);
    }

    public AdminGameHistoryResult getGameHistory(Long gameResultId) {
        return AdminGameHistoryResult.from(gameResultRepository.getByGameResultId(gameResultId));
    }

    public Map<AdminGameHistoryResult, List<AdminGameHistoryParticipantResult>> getHistoryParticipantsByGame(
            List<AdminGameHistoryResult> histories) {
        List<Long> gameResultIds = histories.stream().map(AdminGameHistoryResult::id).toList();
        Map<Long, List<AdminGameHistoryParticipantResult>> participantsByResultId =
                gameResultParticipantRepository.findByGameResultIdIn(gameResultIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                grp -> grp.getGameResult().getId(),
                                Collectors.mapping(AdminGameHistoryParticipantResult::from, Collectors.toList())
                        ));
        return histories.stream().collect(Collectors.toMap(
                history -> history,
                history -> participantsByResultId.getOrDefault(history.id(), List.of())
        ));
    }
}
