package com.team.cops_and_robbers.moderation.infrastructure;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class BannedWordLoader {

    private static final List<String> BANNED_WORD_PATHS = List.of(
            "moderation/banned-words/ko.txt",
            "moderation/banned-words/ja.txt",
            "moderation/banned-words/en.txt"
    );
    private static final String ALLOWED_WORD_PATH = "moderation/allowed-words.txt";
    private static final String COMMENT_PREFIX = "#";

    public List<String> loadBannedWords() {
        return BANNED_WORD_PATHS.stream()
                .flatMap(path -> readWords(path).stream())
                .toList();
    }

    public List<String> loadAllowedWords() {
        return readWords(ALLOWED_WORD_PATH);
    }

    private List<String> readWords(String path) {
        try (InputStream inputStream = new ClassPathResource(path).getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .map(String::strip)
                    .filter(line -> !line.isEmpty() && !line.startsWith(COMMENT_PREFIX))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("금칙어 목록을 읽지 못했습니다: " + path, e);
        }
    }
}
