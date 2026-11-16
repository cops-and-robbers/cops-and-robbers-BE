package com.team.cops_and_robbers.moderation.application;

import com.team.cops_and_robbers.moderation.domain.NormalizedText;
import com.team.cops_and_robbers.moderation.infrastructure.BannedWordLoader;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * 한글·일본어 금칙어는 부분 문자열로, 영어 금칙어는 단어 단위로 찾는다.
 */
@Component
public class ProfanityFilter {

    private static final char MASK = '*';
    private static final Set<Character.UnicodeScript> SUBSTRING_MATCH_SCRIPTS = Set.of(
            Character.UnicodeScript.HANGUL,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HAN
    );

    private final Trie substringTrie;
    private final Trie wholeWordTrie;
    private final Trie allowedTrie;

    // 금칙어를 매칭 방식별로 나눠 정규화한 뒤 trie로 구성
    public ProfanityFilter(BannedWordLoader bannedWordLoader) {
        List<String> bannedWords = bannedWordLoader.loadBannedWords();
        List<String> substringWords = bannedWords.stream().filter(ProfanityFilter::isSubstringMatchWord).toList();
        List<String> wholeWords = bannedWords.stream().filter(word -> !isSubstringMatchWord(word)).toList();

        this.substringTrie = Trie.builder()
                .addKeywords(normalize(substringWords, NormalizedText::forSubstringMatch))
                .build();
        this.wholeWordTrie = Trie.builder()
                .onlyWholeWords()
                .addKeywords(normalize(wholeWords, NormalizedText::forWordMatch))
                .build();
        this.allowedTrie = Trie.builder()
                .addKeywords(normalize(bannedWordLoader.loadAllowedWords(), NormalizedText::forSubstringMatch))
                .build();
    }

    // 금칙어가 하나라도 있는지
    public boolean containsBannedWord(String text) {
        return text != null && !findBannedSpans(text).isEmpty();
    }

    // 금칙어 위치의 공백 외 문자를 *로 가림
    public String mask(String text) {
        if (text == null) {
            return null;
        }
        List<SourceSpan> spans = findBannedSpans(text);
        if (spans.isEmpty()) {
            return text;
        }
        char[] chars = text.toCharArray();
        for (SourceSpan span : spans) {
            for (int i = span.start(); i < span.end(); i++) {
                if (!Character.isWhitespace(chars[i])) {
                    chars[i] = MASK;
                }
            }
        }
        return new String(chars);
    }

    // 허용어에 포함된 매치를 뺀 금칙어의 원본 위치 목록
    private List<SourceSpan> findBannedSpans(String text) {
        List<SourceSpan> spans = new ArrayList<>();

        NormalizedText substringText = NormalizedText.forSubstringMatch(text);
        Collection<Emit> allowedEmits = allowedTrie.parseText(substringText.text());
        substringTrie.parseText(substringText.text()).stream()
                .filter(emit -> !isAllowed(emit, allowedEmits))
                .forEach(emit -> spans.add(SourceSpan.of(substringText, emit)));

        NormalizedText wordText = NormalizedText.forWordMatch(text);
        wholeWordTrie.parseText(wordText.text())
                .forEach(emit -> spans.add(SourceSpan.of(wordText, emit)));
        return spans;
    }

    private static boolean isAllowed(Emit emit, Collection<Emit> allowedEmits) {
        return allowedEmits.stream().anyMatch(allowed -> covers(allowed, emit));
    }

    private static boolean covers(Emit outer, Emit inner) {
        return outer.getStart() <= inner.getStart() && inner.getEnd() <= outer.getEnd();
    }

    private static boolean isSubstringMatchWord(String word) {
        return word.codePoints().anyMatch(codePoint -> SUBSTRING_MATCH_SCRIPTS.contains(Character.UnicodeScript.of(codePoint)));
    }

    private static List<String> normalize(List<String> words, Function<String, NormalizedText> normalizer) {
        return words.stream()
                .map(word -> normalizer.apply(word).text().strip())
                .filter(keyword -> !keyword.isEmpty())
                .distinct()
                .toList();
    }

    private record SourceSpan(int start, int end) {
        static SourceSpan of(NormalizedText normalizedText, Emit emit) {
            return new SourceSpan(normalizedText.sourceStart(emit.getStart()), normalizedText.sourceEnd(emit.getEnd()));
        }
    }
}
