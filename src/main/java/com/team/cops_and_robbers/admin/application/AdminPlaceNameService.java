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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminPlaceNameService {

    private static final int MAX_NEW_LOOKUP_COUNT = 5;

    // 벤더 타임아웃(5초)을 다 기다리면 장애 때 목록이 멈춰 보여, 응답은 3초까지만 기다린다. 늦게 끝난 조회도 저장은 된다
    private static final long LOOKUP_WAIT_MILLIS = 3_000;

    // 실패한 곳은 저장되지 않아 목록을 열 때마다 다시 부르고 장애 알림도 쌓이므로, 이 시간 동안 건너뛴다
    private static final Duration RETRY_DELAY = Duration.ofMinutes(10);

    private final AdminGameService adminGameService;
    private final AreaPlaceNameRepository areaPlaceNameRepository;
    private final GameResultParticipantRepository gameResultParticipantRepository;
    private final GeocodingClient geocodingClient;
    private final Clock clock;

    private final ExecutorService lookupExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Instant> retryAtByKey = new ConcurrentHashMap<>();

    // placeName 과 countryCode 를 함께 물으면 같은 곳을 두 번 조회하게 되어, 조회 중인 곳은 결과를 같이 기다린다
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

    // 끝난 시각 컬럼이 생기기 전 기록은 만든 시각을 끝난 시각으로 본다
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

    // 기다림은 복사본에만 걸어, 응답이 타임아웃돼도 조회와 저장은 끝까지 진행된다
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

    private static <K> Map<K, String> countryCodes(Map<K, AdminAreaPlaceResult> places) {
        return places.entrySet().stream()
                .filter(entry -> entry.getValue().countryCode() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().countryCode()));
    }

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
