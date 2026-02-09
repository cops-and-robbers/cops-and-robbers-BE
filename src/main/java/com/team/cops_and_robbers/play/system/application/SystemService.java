package com.team.cops_and_robbers.play.system.application;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.system.application.dto.ArrestCommand;
import com.team.cops_and_robbers.play.system.application.dto.EscapeCommand;
import com.team.cops_and_robbers.play.system.domain.SystemEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemService {

    private final GameParticipantRepository gameParticipantRepository;

    private final ApplicationEventPublisher eventPublisher;
    private final SystemEventFactory systemEventFactory;

    /**
     * 경찰이 도둑을 체포합니다.
     */
    @Transactional
    public void arrestRobber(ArrestCommand command) {
        GameParticipant police = gameParticipantRepository.getByGameIdAndUserId(command.gameId(), command.policeUserId());
        GameParticipant robber = gameParticipantRepository.getParticipantById(command.robberParticipantId());

        validateArrest(police, robber, command.gameId());

        robber.updateStatus(ParticipantStatus.JAILED);

        int remainingThieves = gameParticipantRepository.countByGameIdAndRobberStatus(
                command.gameId(), ParticipantStatus.ALIVE
        );

        SystemEvent event = systemEventFactory.createArrestEvent(
                command.gameId(), police, robber, remainingThieves
        );
        eventPublisher.publishEvent(event);
    }

    /**
     * 수감된 도둑이 탈옥합니다.
     */
    @Transactional
    public void escapeFromPrison(EscapeCommand command) {
        GameParticipant escapedThief = gameParticipantRepository.getByGameIdAndUserId(
                command.gameId(), command.escapedThiefUserId()
        );

        validateEscape(escapedThief);

        escapedThief.updateStatus(ParticipantStatus.ALIVE);

        SystemEvent event = systemEventFactory.createEscapeEvent(command.gameId(), escapedThief);
        eventPublisher.publishEvent(event);
    }

    private void validateArrest(GameParticipant police, GameParticipant robber, Long gameId) {
        if (!police.isPolice()) {
            throw new ApplicationException(GameParticipantException.ONLY_POLICE_CAN_ARREST);
        }
        if (!robber.isRobber()) {
            throw new ApplicationException(GameParticipantException.ONLY_ROBBER_CAN_BE_ARRESTED);
        }
        if (!robber.isInGame(gameId)) {
            throw new ApplicationException(GameParticipantException.NOT_A_PARTICIPANT);
        }
        if (robber.isJailed()) {
            throw new ApplicationException(GameParticipantException.ALREADY_ARRESTED);
        }
    }

    private void validateEscape(GameParticipant escapedThief) {
        if (!escapedThief.isRobber()) {
            throw new ApplicationException(GameParticipantException.ONLY_ROBBER_CAN_ESCAPE);
        }
        if (!escapedThief.isJailed()) {
            throw new ApplicationException(GameParticipantException.NOT_JAILED);
        }
    }
}
