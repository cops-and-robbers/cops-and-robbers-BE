package com.team.cops_and_robbers.common.infrastructure.discord;

import com.team.cops_and_robbers.common.infrastructure.discord.dto.DiscordEmbed;
import com.team.cops_and_robbers.common.infrastructure.discord.dto.DiscordEmbedField;
import com.team.cops_and_robbers.common.infrastructure.discord.dto.DiscordWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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

    public DiscordNotifier(DiscordProperties discordProperties, RestClient.Builder builder) {
        this.discordProperties = discordProperties;
        this.restClient = builder.build();
    }

    @Async
    public void sendBugReport(String content, String nickname) {
        DiscordWebhookPayload payload = DiscordWebhookPayload.bug(
                DiscordEmbed.bug(content, List.of(
                        DiscordEmbedField.of("제보자", nickname),
                        DiscordEmbedField.of("시각", LocalDateTime.now().format(FORMATTER))
                ))
        );

        send(discordProperties.bug(), payload);
    }

    @Async
    public void sendGeocodingFailure(Double latitude, Double longitude, String cause) {
        DiscordWebhookPayload payload = DiscordWebhookPayload.alert(
                DiscordEmbed.alert("역지오코딩 호출에 실패했습니다.", List.of(
                        DiscordEmbedField.of("좌표", latitude + ", " + longitude),
                        DiscordEmbedField.of("원인", cause),
                        DiscordEmbedField.of("시각", LocalDateTime.now().format(FORMATTER))
                ))
        );

        send(discordProperties.alert(), payload);
    }

    private void send(String webhookUrl, DiscordWebhookPayload payload) {
        if (!StringUtils.hasText(webhookUrl)) {
            return;
        }

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Discord 알림 전송 실패 | type={}", payload.username(), e);
        }
    }
}
