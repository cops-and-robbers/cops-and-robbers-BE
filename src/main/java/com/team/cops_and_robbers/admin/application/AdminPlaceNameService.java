package com.team.cops_and_robbers.admin.application;

import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameAreaResult.AdminCoordinateResult;
import com.team.cops_and_robbers.admin.application.dto.result.game.AdminGameSummaryResult;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingClient;
import com.team.cops_and_robbers.community.post.infrastructure.GeocodingResult;
import com.team.cops_and_robbers.game.area.repository.AreaPlaceNameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 어드민 게임 화면에 보여 줄 플레이 구역의 동네 이름을 찾습니다.
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
    private final GeocodingClient geocodingClient;
    private final Clock clock;

    private final ExecutorService lookupExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Instant> retryAtByKey = new ConcurrentHashMap<>();

    public Map<AdminGameAreaResult, String> getPlaceNamesByArea(List<AdminGameAreaResult> areas) {
        try {
            return findPlaceNamesByArea(areas);
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 구역 동네 이름 조회 실패 | areaCount={}", areas.size(), e);
            return Map.of();
        }
    }

    public Map<AdminGameSummaryResult, String> getPlaceNamesByGame(List<AdminGameSummaryResult> games) {
        try {
            Map<AdminGameSummaryResult, AdminGameAreaResult> areaByGame = adminGameService.getAreasByGameSummary(games);
            Map<AdminGameAreaResult, String> placeNameByArea = findPlaceNamesByArea(List.copyOf(areaByGame.values()));
            return areaByGame.entrySet().stream()
                    .filter(entry -> placeNameByArea.containsKey(entry.getValue()))
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> placeNameByArea.get(entry.getValue())
                    ));
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 게임 목록 동네 이름 조회 실패 | gameCount={}", games.size(), e);
            return Map.of();
        }
    }

    private Map<AdminGameAreaResult, String> findPlaceNamesByArea(List<AdminGameAreaResult> areas) {
        Map<AdminGameAreaResult, RoundedCoordinate> coordinateByArea = new LinkedHashMap<>();
        for (AdminGameAreaResult area : areas) {
            AdminCoordinateResult point = area.representativePoint();
            if (point != null) {
                coordinateByArea.putIfAbsent(area, RoundedCoordinate.from(point));
            }
        }

        Map<String, String> placeNameByKey = findPlaceNames(List.copyOf(coordinateByArea.values()));
        return coordinateByArea.entrySet().stream()
                .filter(entry -> placeNameByKey.containsKey(entry.getValue().key()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> placeNameByKey.get(entry.getValue().key())
                ));
    }

    private Map<String, String> findPlaceNames(List<RoundedCoordinate> coordinates) {
        Map<String, RoundedCoordinate> coordinateByKey = new LinkedHashMap<>();
        coordinates.forEach(coordinate -> coordinateByKey.putIfAbsent(coordinate.key(), coordinate));
        if (coordinateByKey.isEmpty()) {
            return Map.of();
        }

        Map<String, String> placeNameByKey = new HashMap<>();
        areaPlaceNameRepository.findByCoordinateKeyIn(coordinateByKey.keySet())
                .forEach(saved -> placeNameByKey.put(saved.getCoordinateKey(), saved.getPlaceName()));

        Instant now = clock.instant();
        List<RoundedCoordinate> notSaved = coordinateByKey.values().stream()
                .filter(coordinate -> !placeNameByKey.containsKey(coordinate.key()))
                .filter(coordinate -> isRetryable(coordinate.key(), now))
                .limit(MAX_NEW_LOOKUP_COUNT)
                .toList();
        placeNameByKey.putAll(lookUpPlaceNames(notSaved));
        return placeNameByKey;
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
     */
    private Map<String, String> lookUpPlaceNames(List<RoundedCoordinate> coordinates) {
        if (coordinates.isEmpty()) {
            return Map.of();
        }

        Map<String, CompletableFuture<String>> lookupByKey = new LinkedHashMap<>();
        coordinates.forEach(coordinate -> lookupByKey.put(
                coordinate.key(),
                CompletableFuture.supplyAsync(() -> lookUpPlaceName(coordinate), lookupExecutor)
                        .completeOnTimeout(null, LOOKUP_WAIT_MILLIS, TimeUnit.MILLISECONDS)
        ));

        Map<String, String> placeNameByKey = new HashMap<>();
        lookupByKey.forEach((key, lookup) -> {
            String placeName = lookup.join();
            if (placeName != null) {
                placeNameByKey.put(key, placeName);
            }
        });
        return placeNameByKey;
    }

    private String lookUpPlaceName(RoundedCoordinate coordinate) {
        try {
            GeocodingResult result = geocodingClient.reverseGeocode(coordinate.latitude(), coordinate.longitude());
            if (!(result instanceof GeocodingResult.Resolved resolved) || resolved.postAddress().region() == null) {
                delayRetry(coordinate.key());
                return null;
            }

            String placeName = resolved.postAddress().region();
            save(coordinate.key(), placeName);
            return placeName;
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 동네 이름 조회 실패 | key={}", coordinate.key(), e);
            delayRetry(coordinate.key());
            return null;
        }
    }

    /**
     * 저장에 실패해도 찾은 이름은 응답에 씁니다. 다음 조회에서 다시 찾아 저장합니다.
     */
    private void save(String coordinateKey, String placeName) {
        try {
            areaPlaceNameRepository.insertIfAbsent(coordinateKey, placeName, LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            log.warn("[PlaceName] 동네 이름 저장 실패 | key={}", coordinateKey, e);
        }
    }

    private void delayRetry(String coordinateKey) {
        retryAtByKey.put(coordinateKey, clock.instant().plus(RETRY_DELAY));
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
}
