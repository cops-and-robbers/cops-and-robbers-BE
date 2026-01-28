package com.team.cops_and_robbers.play.chat.application;

import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.exception.GameParticipantException;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.play.chat.application.dto.ChatCommand;
import com.team.cops_and_robbers.play.chat.domain.ChatMessage;
import com.team.cops_and_robbers.play.chat.domain.ChatScope;
import com.team.cops_and_robbers.play.chat.domain.ChatSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatPublisher {

    private final RedisTemplate<String, Object> redisTemplate;
    private final GameParticipantRepository gameParticipantRepository;

    /**
     * 1. 채팅 메세지를 받아 적절한 토픽에 라우팅
     * 특이 사항: 현재는 RDB 에서 조회하지만, 게임 시작시 유저 정보를 레디스에 넣는 로직이 완성되면 레디스로 조회로 개선 예정.
     */
    @Transactional(readOnly = true)
    public void processAndRouteMessage(ChatCommand command) {
        GameParticipant participant = gameParticipantRepository.findByIdWithUser(command.participantId())
                .orElseThrow(() -> new ApplicationException(GameParticipantException.PARTICIPANT_NOT_FOUND));
        ChatSender sender = ChatSender.of(command.participantId(), participant.getUser().getNickname(), participant.getTeam());
        ChatMessage chatMessage = ChatMessage.of(command.gameId(), sender, command.message(), command.scope());

        RedisChannel targetChannel = routeTopic(command.scope(), participant.getTeam());
        String destinationTopic = targetChannel.getTopic(command.gameId());
        redisTemplate.convertAndSend(destinationTopic, chatMessage);

        log.debug("Chat Pub: topic={}, sender={}", destinationTopic, sender.nickname());
    }

    private RedisChannel routeTopic(ChatScope scope, Team participantTeam) {
        if (scope == ChatScope.TEAM) {
            return switch (participantTeam) {
                case POLICE -> RedisChannel.CHAT_POLICE;
                case ROBBER -> RedisChannel.CHAT_ROBBER;
            };
        }
        return RedisChannel.CHAT_ALL;
    }

}
