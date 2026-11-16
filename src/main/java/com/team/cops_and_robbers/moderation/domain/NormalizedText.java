package com.team.cops_and_robbers.moderation.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NormalizedText {

    private static final char TOKEN_SEPARATOR = ' ';
    private static final Set<Character.UnicodeScript> SUBSTRING_MATCH_SCRIPTS = Set.of(
            Character.UnicodeScript.HANGUL,
            Character.UnicodeScript.HIRAGANA,
            Character.UnicodeScript.KATAKANA,
            Character.UnicodeScript.HAN
    );

    private final String text;
    private final int[] sourceStarts;
    private final int[] sourceEnds;

    // 정규화된 문자로 텍스트를 만들고, 각 char의 원본 위치를 기록
    private NormalizedText(List<MappedChar> chars) {
        StringBuilder builder = new StringBuilder();
        chars.forEach(mappedChar -> builder.appendCodePoint(mappedChar.codePoint()));

        this.text = builder.toString();
        this.sourceStarts = new int[text.length()];
        this.sourceEnds = new int[text.length()];

        int index = 0;
        for (MappedChar mappedChar : chars) {
            int next = index + Character.charCount(mappedChar.codePoint());
            Arrays.fill(sourceStarts, index, next, mappedChar.sourceStart());
            Arrays.fill(sourceEnds, index, next, mappedChar.sourceEnd());
            index = next;
        }
    }

    // 공백·기호를 걷어내고 자모를 조합한 부분 문자열 검사용 텍스트
    public static NormalizedText forSubstringMatch(String original) {
        List<MappedChar> chars = new ArrayList<>();
        boolean previousWasSingleLetter = false;
        int previousTokenEnd = 0;
        int tokenStart = skipWhitespace(original, 0);

        while (tokenStart < original.length()) {
            int tokenEnd = skipNonWhitespace(original, tokenStart);
            List<MappedChar> letters = foldLetters(original, tokenStart, tokenEnd);

            if (!letters.isEmpty()) {
                // "ㅅ ㅂ"처럼 한 글자씩 띄운 우회 입력은 붙여서 검사
                boolean bothSingleLetters = (previousWasSingleLetter && letters.size() == 1);
                if (!chars.isEmpty() && !bothSingleLetters) {
                    chars.add(new MappedChar(TOKEN_SEPARATOR, previousTokenEnd, tokenStart));
                }
                chars.addAll(letters);
                previousWasSingleLetter = (letters.size() == 1);
                previousTokenEnd = tokenEnd;
            }
            tokenStart = skipWhitespace(original, tokenEnd);
        }
        return new NormalizedText(HangulJamoComposer.compose(chars));
    }

    // leet 표기를 알파벳으로 되돌린 단어 단위 검사용 텍스트
    public static NormalizedText forWordMatch(String original) {
        List<MappedChar> chars = fold(original, 0, original.length());
        separateSubstringScripts(chars);
        int tokenStart = skipNonWord(chars, 0);

        while (tokenStart < chars.size()) {
            int tokenEnd = skipWord(chars, tokenStart);
            replaceLeetLetters(chars, tokenStart, tokenEnd);
            tokenStart = skipNonWord(chars, tokenEnd);
        }
        return new NormalizedText(chars);
    }

    public static boolean isSubstringMatchScript(int codePoint) {
        return SUBSTRING_MATCH_SCRIPTS.contains(Character.UnicodeScript.of(codePoint));
    }

    // "fuck과"처럼 비속어 뒤에 붙은 한글·일본어·한자를 단어 경계로 바꿈
    private static void separateSubstringScripts(List<MappedChar> chars) {
        chars.replaceAll(mappedChar -> isSubstringMatchScript(mappedChar.codePoint())
                ? mappedChar.withCodePoint(TOKEN_SEPARATOR)
                : mappedChar);
    }

    public String text() {
        return text;
    }

    public int sourceStart(int index) {
        return sourceStarts[index];
    }

    public int sourceEnd(int index) {
        return sourceEnds[index];
    }

    // 공백이 끝나는 위치 반환
    private static int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.codePointAt(index))) {
            index += Character.charCount(text.codePointAt(index));
        }
        return index;
    }

    // 공백이 아닌 구간이 끝나는 위치 반환
    private static int skipNonWhitespace(String text, int index) {
        while (index < text.length() && !Character.isWhitespace(text.codePointAt(index))) {
            index += Character.charCount(text.codePointAt(index));
        }
        return index;
    }

    // 단어 문자 구간이 끝나는 위치 반환
    private static int skipWord(List<MappedChar> chars, int index) {
        while (index < chars.size() && isWordChar(chars.get(index).codePoint())) {
            index++;
        }
        return index;
    }

    // 단어 문자가 아닌 구간이 끝나는 위치 반환
    private static int skipNonWord(List<MappedChar> chars, int index) {
        while (index < chars.size() && !isWordChar(chars.get(index).codePoint())) {
            index++;
        }
        return index;
    }

    // 구간을 정규화하고 문자만 남김
    private static List<MappedChar> foldLetters(String original, int from, int to) {
        return fold(original, from, to).stream()
                .filter(mappedChar -> Character.isLetter(mappedChar.codePoint()))
                .toList();
    }

    // 구간을 코드포인트 단위로 정규화하며 원본 위치 유지
    private static List<MappedChar> fold(String original, int from, int to) {
        List<MappedChar> chars = new ArrayList<>(to - from);
        int index = from;
        while (index < to) {
            int codePoint = original.codePointAt(index);
            int end = index + Character.charCount(codePoint);
            for (int foldedCodePoint : foldCodePoint(codePoint).codePoints().toArray()) {
                chars.add(new MappedChar(foldedCodePoint, index, end));
            }
            index = end;
        }
        return chars;
    }

    // NFKC + 소문자화, 한글은 호환 자모가 조합용 자모로 바뀌지 않게 그대로 둠
    private static String foldCodePoint(int codePoint) {
        String character = Character.toString(codePoint);
        if (isHangul(codePoint)) {
            return character;
        }
        return Normalizer.normalize(character, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }

    // 토큰의 leet 문자를 알파벳으로 치환, 숫자만 있는 토큰은 그대로 둠
    private static void replaceLeetLetters(List<MappedChar> chars, int from, int to) {
        List<MappedChar> token = chars.subList(from, to);
        if (token.stream().noneMatch(mappedChar -> Character.isLetter(mappedChar.codePoint()))) {
            return;
        }
        token.replaceAll(mappedChar -> mappedChar.withCodePoint(leetToLetter(mappedChar.codePoint())));
    }

    // 문자·숫자 또는 leet 기호인지
    private static boolean isWordChar(int codePoint) {
        return Character.isLetterOrDigit(codePoint) || leetToLetter(codePoint) != codePoint;
    }

    // leet 문자면 대응 알파벳, 아니면 그대로 반환
    private static int leetToLetter(int codePoint) {
        return switch (codePoint) {
            case '0' -> 'o';
            case '1' -> 'i';
            case '3' -> 'e';
            case '4', '@' -> 'a';
            case '5', '$' -> 's';
            case '7' -> 't';
            default -> codePoint;
        };
    }

    // 한글 스크립트 문자인지
    private static boolean isHangul(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HANGUL;
    }
}
