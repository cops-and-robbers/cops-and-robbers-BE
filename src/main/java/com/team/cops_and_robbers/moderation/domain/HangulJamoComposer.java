package com.team.cops_and_robbers.moderation.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 풀어 쓴 호환 자모(ㅅㅣㅂㅏㄹ)를 음절(시발)로 합친다. 모음이 없는 자모(ㅅㅂ)는 그대로 둔다.
 */
final class HangulJamoComposer {

    private static final String CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    private static final String JONGSEONG = " ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";
    private static final int FIRST_JUNGSEONG = 'ㅏ';
    private static final int LAST_JUNGSEONG = 'ㅣ';
    private static final int SYLLABLE_BASE = 0xAC00;
    private static final int JUNGSEONG_COUNT = 21;
    private static final int JONGSEONG_COUNT = 28;

    private HangulJamoComposer() {
    }

    static List<MappedChar> compose(List<MappedChar> chars) {
        List<MappedChar> composed = new ArrayList<>(chars.size());
        int index = 0;
        while (index < chars.size()) {
            int choseong = CHOSEONG.indexOf(chars.get(index).codePoint());
            if (choseong < 0 || !isJungseongAt(chars, index + 1)) {
                composed.add(chars.get(index));
                index++;
                continue;
            }

            int jungseong = chars.get(index + 1).codePoint() - FIRST_JUNGSEONG;
            int jongseong = jongseongAt(chars, index + 2);
            int length = jongseong > 0 ? 3 : 2;
            int syllable = SYLLABLE_BASE + (choseong * JUNGSEONG_COUNT + jungseong) * JONGSEONG_COUNT + jongseong;
            composed.add(new MappedChar(
                    syllable, chars.get(index).sourceStart(), chars.get(index + length - 1).sourceEnd()));
            index += length;
        }
        return composed;
    }

    /** 뒤에 모음이 오면 다음 음절의 초성이므로 받침으로 쓰지 않는다. */
    private static int jongseongAt(List<MappedChar> chars, int index) {
        if (index >= chars.size() || isJungseongAt(chars, index + 1)) {
            return 0;
        }
        return Math.max(JONGSEONG.indexOf(chars.get(index).codePoint()), 0);
    }

    private static boolean isJungseongAt(List<MappedChar> chars, int index) {
        if (index >= chars.size()) {
            return false;
        }
        int codePoint = chars.get(index).codePoint();
        return FIRST_JUNGSEONG <= codePoint && codePoint <= LAST_JUNGSEONG;
    }
}
