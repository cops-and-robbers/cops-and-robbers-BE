package com.team.cops_and_robbers.community.post.application.dto.command;

import com.team.cops_and_robbers.common.exception.ApplicationException;
import com.team.cops_and_robbers.common.exception.CommonException;
import com.team.cops_and_robbers.community.post.domain.CommunityPostScope;
import com.team.cops_and_robbers.community.post.domain.CommunityPostSort;
import com.team.cops_and_robbers.community.post.exception.CommunityPostException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 목록은 국가 단위로 나눠서 내려준다. 국가 코드는 GET /api/community-posts/country 로 먼저 조회한다.
 * 웹의 영어 페이지처럼 "특정 국가를 뺀 전부"가 필요한 경우에만 excludeCountryCodes 를 대신 쓴다.
 */
public record CommunityPostListCommand(
        String cursor,
        int size,
        CommunityPostScope scope,
        CommunityPostSort sort,
        String countryCode,
        List<String> excludeCountryCodes,
        Double latitude,
        Double longitude,
        String keyword
) {
    private static final int MAX_SIZE = 100;
    private static final int MIN_KEYWORD_LENGTH = 2;
    private static final int MAX_EXCLUDE_COUNTRY_CODES = 20;
    /** 커서에 국가 조건을 담을 때 구분자(|)가 섞이면 커서가 깨지므로 형식을 먼저 막는다. */
    private static final Pattern COUNTRY_CODE = Pattern.compile("[A-Z]{2}");

    public CommunityPostListCommand {
        if (size < 1 || size > MAX_SIZE) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        countryCode = normalizeCountryCode(countryCode);
        excludeCountryCodes = normalizeExcludeCountryCodes(excludeCountryCodes);
        validateCountryFilter(countryCode, excludeCountryCodes);
        if (scope != CommunityPostScope.ALL) {
            throw new ApplicationException(CommunityPostException.UNSUPPORTED_LIST_SCOPE);
        }
        validateCoordinates(sort, latitude, longitude);
        validateKeyword(keyword);
    }

    /** 국가 조회와 제외 조회의 커서가 섞이지 않도록 조회 범위를 문자열 하나로 만든다. */
    public String countryScopeKey() {
        if (countryCode != null) {
            return countryCode;
        }
        return "!" + String.join(",", excludeCountryCodes);
    }

    private static String normalizeCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return null;
        }
        return validated(countryCode);
    }

    /** 순서가 달라도 같은 조회이므로 정렬해서 담는다. 그래야 커서도 같은 값이 나온다. */
    private static List<String> normalizeExcludeCountryCodes(List<String> excludeCountryCodes) {
        if (excludeCountryCodes == null) {
            return List.of();
        }
        List<String> normalized = excludeCountryCodes.stream()
                .filter(StringUtils::hasText)
                .map(CommunityPostListCommand::validated)
                .distinct()
                .sorted()
                .toList();
        if (normalized.size() > MAX_EXCLUDE_COUNTRY_CODES) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        return normalized;
    }

    private static String validated(String countryCode) {
        String normalized = countryCode.trim().toUpperCase(Locale.ROOT);
        if (!COUNTRY_CODE.matcher(normalized).matches()) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        return normalized;
    }

    private static void validateCountryFilter(String countryCode, List<String> excludeCountryCodes) {
        if (countryCode != null && !excludeCountryCodes.isEmpty()) {
            throw new ApplicationException(CommunityPostException.CONFLICTING_COUNTRY_FILTER);
        }
        if (countryCode == null && excludeCountryCodes.isEmpty()) {
            throw new ApplicationException(CommunityPostException.COUNTRY_NOT_SPECIFIED);
        }
    }

    /** 공백만 오면 검색 없이 전체를 내려주고, 한 글자는 결과가 너무 많아 거절한다. */
    private static void validateKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return;
        }
        if (keyword.trim().length() < MIN_KEYWORD_LENGTH) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
    }

    private static void validateCoordinates(CommunityPostSort sort, Double latitude, Double longitude) {
        boolean given = latitude != null || longitude != null;
        if (sort == CommunityPostSort.DISTANCE && (latitude == null || longitude == null)) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
        if (sort != CommunityPostSort.DISTANCE && given) {
            throw new ApplicationException(CommonException.INVALID_QUERY_PARAMETER);
        }
    }
}
