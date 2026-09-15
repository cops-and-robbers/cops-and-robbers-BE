package com.team.cops_and_robbers.loadtest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.loadtest.LoadTestSeeder.PlayerCredential;
import com.team.cops_and_robbers.loadtest.LoadTestSeeder.RoomSeedResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * [부하 테스트용 시딩 러너]
 * k6에서 사용할 players.json 파일을 생성
 * loadtest.seed.enabled=true 일 때만 실행됨
 *
 * [실행 방법]
 * SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --args='--loadtest.seed.enabled=true'
 */
@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "loadtest.seed.enabled", havingValue = "true")
public class LoadTestDataLoader implements CommandLineRunner {

    private static final List<RoomSpec> ROOMS = List.of(
            new RoomSpec("A", "LTA001", 20),
            new RoomSpec("B", "LTB001", 100),
            new RoomSpec("C", "LTC001", 50)
    );

    private final LoadTestSeeder seeder;
    private final GameRepository gameRepository;
    private final ObjectMapper objectMapper;

    @Value("${loadtest.seed.output:./loadtest/players.json}")
    private String outputPath;

    @Override
    public void run(String... args) throws Exception {
        if (skipOrFailOnExistingData()) {
            return;
        }

        log.info("========== 부하테스트 데이터 시딩 시작 ==========");

        List<PlayerCredential> allPlayers = new ArrayList<>();

        int userOffset = 0;
        for (RoomSpec room : ROOMS) {
            RoomSeedResult result = seeder.seedRoom(room.label(), room.inviteCode(), room.size(), userOffset);
            allPlayers.addAll(result.players());
            userOffset += room.size();

            // 참가자 커밋이 끝난 뒤에야 캐시가 전원을 읽어갈 수 있으므로 시작과 분리해 호출
            seeder.startGame(result.gameId());
            seeder.scheduleAndLoadCache(result.gameId());
        }

        writePlayersFile(allPlayers);

        log.info("========== 부하테스트 데이터 시딩 완료: 총 {}명 ==========", allPlayers.size());
    }

    /**
     * 이미 시딩된 상태면 건너뛰고, 중간에 끊긴 상태면 실패시킨다.
     * <p>
     * 방 하나만 확인하면 A는 커밋됐는데 B/C에서 실패한 경우를 완료로 오인해,
     * 다음 실행이 통째로 건너뛰면서 players.json 없이 반쪽짜리 데이터만 남는다.
     * 방 세 개와 토큰 파일이 모두 있어야 완료로 본다.
     *
     * @return 시딩을 건너뛰어야 하면 true
     */
    private boolean skipOrFailOnExistingData() {
        long seededRooms = ROOMS.stream()
                .filter(room -> gameRepository.existsByInviteCode(room.inviteCode()))
                .count();
        boolean tokenFileExists = Files.exists(Path.of(outputPath));

        if (seededRooms == ROOMS.size() && tokenFileExists) {
            log.info("[LoadTest] 이미 시딩된 데이터가 있어 건너뜁니다. 다시 만들려면 정리 후 재실행하세요. (README 참고)");
            return true;
        }

        if (seededRooms > 0 || tokenFileExists) {
            throw new IllegalStateException(
                    "부하테스트 데이터가 불완전합니다. 방 %d/%d, 토큰 파일 %s. 정리 후 다시 실행하세요. (README 참고)"
                            .formatted(seededRooms, ROOMS.size(), tokenFileExists ? "있음" : "없음")
            );
        }

        return false;
    }

    private void writePlayersFile(List<PlayerCredential> players) throws Exception {
        Path path = Path.of(outputPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), players);
        log.info("[LoadTest] 토큰 파일 생성: {} ({}명)", path.toAbsolutePath(), players.size());
    }

    private record RoomSpec(String label, String inviteCode, int size) {
    }
}
