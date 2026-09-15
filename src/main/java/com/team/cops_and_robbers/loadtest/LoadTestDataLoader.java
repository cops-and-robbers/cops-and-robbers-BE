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
        if (gameRepository.existsByInviteCode(ROOMS.get(0).inviteCode())) {
            log.info("[LoadTest] 이미 시딩된 데이터가 있어 건너뜁니다. 다시 만들려면 DB를 비우세요.");
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
