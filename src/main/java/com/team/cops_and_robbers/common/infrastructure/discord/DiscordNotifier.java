package com.team.cops_and_robbers.common.infrastructure.discord;

import com.team.cops_and_robbers.common.infrastructure.discord.dto.DiscordEmbed;
import com.team.cops_and_robbers.common.infrastructure.discord.dto.DiscordEmbedField;
import com.team.cops_and_robbers.common.infrastructure.discord.dto.DiscordWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class DiscordNotifier {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;
    private final DiscordProperties discordProperties;

    public DiscordNotifier(DiscordProperties discordProperties) {
        this.discordProperties = discordProperties;
        this.restClient = RestClient.create();
    }

    @Async
    public void sendBugReport(String content, String nickname) {
        DiscordWebhookPayload payload = DiscordWebhookPayload.of(
                DiscordEmbed.bug(content, List.of(
                        DiscordEmbedField.of("제보자", nickname),
                        DiscordEmbedField.of("시각", LocalDateTime.now().format(FORMATTER))
                ))
        );

        send(discordProperties.bug(), payload);
    }

    private void send(String webhookUrl, DiscordWebhookPayload payload) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Discord 알림 전송 실패 | url={}", webhookUrl, e);
        }
    }
}
