package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.result.game.AdminAreaPlaceResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult.AdminCoordinateResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameSummaryResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminPlayCountryResult;
import com.team.cops_and_robbers.admin.application.dto.result.user.AdminUserResult;
import com.team.cops_and_robbers.common.util.TimestampUtil;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingClient;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.game.area.repository.AreaPlaceNameRepository;
import com.team.cops_and_robbers.history.domain.GameResult;
import com.team.cops_and_robbers.history.domain.GameResultParticipant;
import com.team.cops_and_robbers.history.repository.GameResultParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 어드민 게임 화면에 보여 줄 플레이 구역의 동네 이름과 국가를 찾습니다.
 * <p>
 * 한 번 찾은 이름은 좌표별로 저장해 두고, 저장되지 않은 곳만 주소 조회를 부릅니다.
 * 주소 조회는 외부 호출이라 트랜잭션을 걸지 않고, 저장은 이름을 찾은 곳마다 따로 커밋합니다.
 * 이름을 못 찾아도 어드민 조회는 실패시키지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlaceNameService {

    /**
     * 처음 보는 구역이 많은 목록을 열어도 응답이 길어지지 않도록 한 번에 새로 찾는 곳을 제한합니다.
     * 나머지는 다음 조회에서 채워집니다.
     */
    private static final int MAX_NEW_LOOKUP_COUNT = 5;

    /**
     * 벤더 타임아웃(5초)을 다 기다리면 장애 때 목록이 멈춰 보여서, 응답은 이 시간까지만 기다립니다.
     * 늦게 끝난 조회도 저장은 되므로 다음 조회에서 이름이 나옵니다.
     */
    private static final long LOOKUP_WAIT_MILLIS = 3_000;

    /**
     * 주소가 없거나 조회에 실패한 곳은 저장하지 않아서, 그대로 두면 목록을 열 때마다 같은 곳을 다시 부르고
     * 장애 알림도 그만큼 쌓입니다. 이 시간 동안은 건너뜁니다.
     */
    private static final Duration RETRY_DELAY = Duration.ofMinutes(10);

    private final AdminGameService adminGameService;
    private final AreaPlaceNameRepository areaPlaceNameRepository;
    private final GameResultParticipantRepository gameResultParticipantRepository;
    private final GeocodingClient geocodingClient;
    private final Clock clock;

    private final ExecutorService lookupExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Instant> retryAtByKey = new ConcurrentHashMap<>();

    /**
     * 지금 주소를 조회하고 있는 곳입니다. 한 요청 안에서 placeName 과 countryCode 필드를 함께 물으면
     * 같은 곳을 두 번 조회하게 되어, 조회 중인 곳은 그 결과를 같이 기다립니다.
     */
    private final Map<String, CompletableFuture<AdminAreaPlaceResult>> lookupByKey = new ConcurrentHashMap<>();

    public Map<AdminGameAreaResult, AdminAreaPlaceResult> getPlacesByArea(List<AdminGameAreaResult> areas) {
        try {
            return findPlacesByArea(areas);
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 구역 동네 이름 조회 실패 | areaCount={}", areas.size(), e);
            return Map.of();
        }
    }

    public Map<AdminGameAreaResult, String> getPlaceNamesByArea(List<AdminGameAreaResult> areas) {
        return placeNames(getPlacesByArea(areas));
    }

    public Map<AdminGameAreaResult, String> getCountryCodesByArea(List<AdminGameAreaResult> areas) {
        return countryCodes(getPlacesByArea(areas));
    }

    public Map<AdminGameSummaryResult, AdminAreaPlaceResult> getPlacesByGame(List<AdminGameSummaryResult> games) {
        try {
            Map<AdminGameSummaryResult, AdminGameAreaResult> areaByGame = adminGameService.getAreasByGameSummary(games);
            Map<AdminGameAreaResult, AdminAreaPlaceResult> placeByArea = findPlacesByArea(List.copyOf(areaByGame.values()));
            return areaByGame.entrySet().stream()
                    .filter(entry -> placeByArea.containsKey(entry.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> placeByArea.get(entry.getValue())
                    ));
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 게임 목록 동네 이름 조회 실패 | gameCount={}", games.size(), e);
            return Map.of();
        }
    }

    public Map<AdminGameSummaryResult, String> getPlaceNamesByGame(List<AdminGameSummaryResult> games) {
        return placeNames(getPlacesByGame(games));
    }

    public Map<AdminGameSummaryResult, String> getCountryCodesByGame(List<AdminGameSummaryResult> games) {
        return countryCodes(getPlacesByGame(games));
    }

    /**
     * 유저마다 끝까지 마친 게임을 플레이 구역의 국가로 묶어 셉니다. 많이 한 국가가 앞이고, 판 수가 같으면 최근에 한 국가가 앞입니다.
     * <p>
     * 국가를 아직 찾지 못한 판은 countryCode 가 null 인 줄 하나로 묶어 맨 뒤에 둡니다. 다른 필드처럼 한 번에 새로 찾는 곳은
     * {@link #MAX_NEW_LOOKUP_COUNT} 곳뿐이라 처음에는 이 줄이 클 수 있고, 다시 조회할수록 줄어듭니다.
     * 좌표를 남기지 않던 시절의 판은 국가를 알 수 없어 세지 않습니다.
     */
    public Map<AdminUserResult, List<AdminPlayCountryResult>> getPlayCountriesByUser(List<AdminUserResult> users) {
        try {
            return findPlayCountriesByUser(users);
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 유저 플레이 국가 조회 실패 | userCount={}", users.size(), e);
            return Map.of();
        }
    }

    private Map<AdminUserResult, List<AdminPlayCountryResult>> findPlayCountriesByUser(List<AdminUserResult> users) {
        List<Long> userIds = users.stream().map(AdminUserResult::id).toList();
        // 이벤트 게임에서 다시 들어오면 같은 판에 행이 여러 개라 판 단위로 한 번만 센다
        Map<Long, Map<Long, GameResult>> resultsByUserId = new HashMap<>();
        for (GameResultParticipant participant : gameResultParticipantRepository.findCompletedWithResultByUserIdIn(userIds)) {
            GameResult result = participant.getGameResult();
            resultsByUserId.computeIfAbsent(participant.getUserId(), id -> new LinkedHashMap<>())
                    .putIfAbsent(result.getId(), result);
        }

        Map<GameResult, RoundedCoordinate> coordinateByResult = new HashMap<>();
        resultsByUserId.values().forEach(results -> results.values().forEach(result -> {
            AdminCoordinateResult point = representativePoint(result);
            if (point != null) {
                coordinateByResult.put(result, RoundedCoordinate.from(point));
            }
        }));
        Map<String, AdminAreaPlaceResult> placeByKey = findPlaces(List.copyOf(coordinateByResult.values()));

        return users.stream().collect(Collectors.toMap(
                user -> user,
                user -> countByCountry(
                        resultsByUserId.getOrDefault(user.id(), Map.of()).values(),
                        coordinateByResult,
                        placeByKey
                )
        ));
    }

    private List<AdminPlayCountryResult> countByCountry(
            Iterable<GameResult> results,
            Map<GameResult, RoundedCoordinate> coordinateByResult,
            Map<String, AdminAreaPlaceResult> placeByKey
    ) {
        Map<String, CountryCount> countByCountry = new HashMap<>();
        for (GameResult result : results) {
            RoundedCoordinate coordinate = coordinateByResult.get(result);
            if (coordinate == null) {
                continue;
            }
            AdminAreaPlaceResult place = placeByKey.get(coordinate.key());
            String countryCode = place == null ? null : place.countryCode();
            countByCountry.computeIfAbsent(countryCode, CountryCount::new).add(playedAt(result));
        }
        return countByCountry.values().stream()
                .sorted(Comparator.comparing((CountryCount count) -> count.countryCode == null)
                        .thenComparing(CountryCount::games, Comparator.reverseOrder())
                        .thenComparing(CountryCount::lastPlayedAt, Comparator.reverseOrder()))
                .map(CountryCount::toResult)
                .toList();
    }

    private static AdminCoordinateResult representativePoint(GameResult result) {
        if (result.getPlaygroundCenter() == null && result.getPlaygroundPolygon() == null) {
            return null;
        }
        return AdminGameAreaResult.from(result).representativePoint();
    }

    /** 끝난 시각 컬럼이 생기기 전 기록은 만든 시각이 끝난 시각입니다(어드민 게임 기록과 같은 기준). */
    private static LocalDateTime playedAt(GameResult result) {
        return result.getEndedAt() != null ? result.getEndedAt() : result.getCreatedAt();
    }

    private Map<AdminGameAreaResult, AdminAreaPlaceResult> findPlacesByArea(List<AdminGameAreaResult> areas) {
        Map<AdminGameAreaResult, RoundedCoordinate> coordinateByArea = new LinkedHashMap<>();
        for (AdminGameAreaResult area : areas) {
            AdminCoordinateResult point = area.representativePoint();
            if (point != null) {
                coordinateByArea.putIfAbsent(area, RoundedCoordinate.from(point));
            }
        }

        Map<String, AdminAreaPlaceResult> placeByKey = findPlaces(List.copyOf(coordinateByArea.values()));
        return coordinateByArea.entrySet().stream()
                .filter(entry -> placeByKey.containsKey(entry.getValue().key()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> placeByKey.get(entry.getValue().key())
                ));
    }

    private Map<String, AdminAreaPlaceResult> findPlaces(List<RoundedCoordinate> coordinates) {
        Map<String, RoundedCoordinate> coordinateByKey = new LinkedHashMap<>();
        coordinates.forEach(coordinate -> coordinateByKey.putIfAbsent(coordinate.key(), coordinate));
        if (coordinateByKey.isEmpty()) {
            return Map.of();
        }

        Map<String, AdminAreaPlaceResult> placeByKey = new HashMap<>();
        areaPlaceNameRepository.findByCoordinateKeyIn(coordinateByKey.keySet())
                .forEach(saved -> placeByKey.put(
                        saved.getCoordinateKey(),
                        new AdminAreaPlaceResult(saved.getPlaceName(), saved.getCountryCode())
                ));

        Instant now = clock.instant();
        List<RoundedCoordinate> notSaved = coordinateByKey.values().stream()
                .filter(coordinate -> !placeByKey.containsKey(coordinate.key()))
                .filter(coordinate -> isRetryable(coordinate.key(), now))
                .limit(MAX_NEW_LOOKUP_COUNT)
                .toList();
        placeByKey.putAll(lookUpPlaces(notSaved));
        return placeByKey;
    }

    private boolean isRetryable(String coordinateKey, Instant now) {
        Instant retryAt = retryAtByKey.get(coordinateKey);
        if (retryAt == null) {
            return true;
        }
        if (now.isBefore(retryAt)) {
            return false;
        }
        retryAtByKey.remove(coordinateKey, retryAt);
        return true;
    }

    /**
     * 주소 조회는 벤더마다 수 초가 걸릴 수 있어 곳마다 기다리면 그만큼 응답이 길어지므로 동시에 부릅니다.
     * 이미 조회 중인 곳은 새로 부르지 않고 그 결과를 같이 기다립니다.
     */
    private Map<String, AdminAreaPlaceResult> lookUpPlaces(List<RoundedCoordinate> coordinates) {
        if (coordinates.isEmpty()) {
            return Map.of();
        }

        Map<String, CompletableFuture<AdminAreaPlaceResult>> lookups = new LinkedHashMap<>();
        coordinates.forEach(coordinate -> lookups.put(
                coordinate.key(),
                lookupOf(coordinate).completeOnTimeout(null, LOOKUP_WAIT_MILLIS, TimeUnit.MILLISECONDS)
        ));

        Map<String, AdminAreaPlaceResult> placeByKey = new HashMap<>();
        lookups.forEach((key, lookup) -> {
            AdminAreaPlaceResult place = lookup.join();
            if (place != null) {
                placeByKey.put(key, place);
            }
        });
        return placeByKey;
    }

    /**
     * 응답을 기다리는 시간과 상관없이 조회는 끝까지 진행되어 저장되도록, 기다림은 복사본에만 걸어 돌려줍니다.
     * 조회 중 표시는 조회를 시작하기 전에 넣고 끝나면 그 표시만 지워서, 끝난 조회가 남아 다시 조회를 막지 않게 합니다.
     */
    private CompletableFuture<AdminAreaPlaceResult> lookupOf(RoundedCoordinate coordinate) {
        String key = coordinate.key();
        CompletableFuture<AdminAreaPlaceResult> created = new CompletableFuture<>();
        CompletableFuture<AdminAreaPlaceResult> running = lookupByKey.putIfAbsent(key, created);
        if (running != null) {
            return running.copy();
        }
        CompletableFuture.supplyAsync(() -> lookUpPlace(coordinate), lookupExecutor)
                .whenComplete((place, error) -> {
                    lookupByKey.remove(key, created);
                    created.complete(error == null ? place : null);
                });
        return created.copy();
    }

    private AdminAreaPlaceResult lookUpPlace(RoundedCoordinate coordinate) {
        try {
            GeocodingResult result = geocodingClient.reverseGeocode(coordinate.latitude(), coordinate.longitude());
            if (!(result instanceof GeocodingResult.Resolved resolved) || resolved.postAddress().region() == null) {
                delayRetry(coordinate.key());
                return null;
            }

            AdminAreaPlaceResult place = new AdminAreaPlaceResult(
                    resolved.postAddress().region(),
                    resolved.postAddress().countryCode()
            );
            save(coordinate.key(), place);
            return place;
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 동네 이름 조회 실패 | key={}", coordinate.key(), e);
            delayRetry(coordinate.key());
            return null;
        }
    }

    /**
     * 저장에 실패해도 찾은 이름은 응답에 씁니다. 다음 조회에서 다시 찾아 저장합니다.
     */
    private void save(String coordinateKey, AdminAreaPlaceResult place) {
        try {
            areaPlaceNameRepository.insertIfAbsent(coordinateKey, place.placeName(), place.countryCode(), LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 동네 이름 저장 실패 | key={}", coordinateKey, e);
        }
    }

    private void delayRetry(String coordinateKey) {
        retryAtByKey.put(coordinateKey, clock.instant().plus(RETRY_DELAY));
    }

    private static <K> Map<K, String> placeNames(Map<K, AdminAreaPlaceResult> places) {
        return places.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().placeName()));
    }

    /** 국가를 모르는 곳은 빼서 돌려줍니다. 응답에서는 null 로 내려갑니다. */
    private static <K> Map<K, String> countryCodes(Map<K, AdminAreaPlaceResult> places) {
        return places.entrySet().stream()
                .filter(entry -> entry.getValue().countryCode() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().countryCode()));
    }

    /**
     * 소수 셋째 자리까지 반올림한 좌표입니다. 위도 0.001도는 약 111m 라 약 100m 안의 구역은 같은 이름을 씁니다.
     * 저장한 이름이 키와 정확히 대응하도록 주소 조회도 반올림한 좌표로 부릅니다.
     */
    private record RoundedCoordinate(double latitude, double longitude) {

        private static final double SCALE = 1_000.0;

        static RoundedCoordinate from(AdminCoordinateResult point) {
            return new RoundedCoordinate(round(point.latitude()), round(point.longitude()));
        }

        String key() {
            return String.format(Locale.ROOT, "%.3f,%.3f", latitude, longitude);
        }

        private static double round(double value) {
            return Math.round(value * SCALE) / SCALE;
        }
    }

    /** 한 국가에서 한 판 수와 마지막 시각을 모읍니다. countryCode 가 null 이면 아직 국가를 못 찾은 판들입니다. */
    private static final class CountryCount {

        private final String countryCode;
        private int games;
        private LocalDateTime lastPlayedAt;

        private CountryCount(String countryCode) {
            this.countryCode = countryCode;
        }

        private void add(LocalDateTime playedAt) {
            games++;
            if (lastPlayedAt == null || (playedAt != null && playedAt.isAfter(lastPlayedAt))) {
                lastPlayedAt = playedAt;
            }
        }

        private int games() {
            return games;
        }

        private LocalDateTime lastPlayedAt() {
            return Objects.requireNonNullElse(lastPlayedAt, LocalDateTime.MIN);
        }

        private AdminPlayCountryResult toResult() {
            return new AdminPlayCountryResult(
                    countryCode,
                    games,
                    lastPlayedAt == null ? null : TimestampUtil.toIsoString(lastPlayedAt)
            );
        }
    }
}
