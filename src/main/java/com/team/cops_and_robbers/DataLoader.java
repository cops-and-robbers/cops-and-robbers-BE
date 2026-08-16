package com.team.cops_and_robbers;

import com.team.cops_and_robbers.auth.infrastructure.jwt.JwtTokenProvider;
import com.team.cops_and_robbers.auth.repository.RefreshTokenRepository;
import com.team.cops_and_robbers.community.application.dto.command.CommunityPostCreateCommand;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.domain.PostAddress;
import com.team.cops_and_robbers.community.repository.CommunityPostRepository;
import com.team.cops_and_robbers.game.area.application.dto.GameAreaData;
import com.team.cops_and_robbers.game.area.domain.GameArea;
import com.team.cops_and_robbers.game.area.repository.GameAreaRepository;
import com.team.cops_and_robbers.game.game.application.dto.command.GameCreateCommand;
import com.team.cops_and_robbers.game.game.domain.Game;
import com.team.cops_and_robbers.game.game.repository.GameRepository;
import com.team.cops_and_robbers.game.participant.domain.GameParticipant;
import com.team.cops_and_robbers.game.participant.domain.ParticipantStatus;
import com.team.cops_and_robbers.game.participant.domain.Team;
import com.team.cops_and_robbers.game.participant.repository.GameParticipantRepository;
import com.team.cops_and_robbers.user.domain.DeviceType;
import com.team.cops_and_robbers.user.domain.SocialType;
import com.team.cops_and_robbers.user.domain.User;
import com.team.cops_and_robbers.user.domain.UserDevice;
import com.team.cops_and_robbers.user.repository.UserDeviceRepository;
import com.team.cops_and_robbers.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final GameRepository gameRepository;
    private final GameAreaRepository gameAreaRepository;
    private final GameParticipantRepository gameParticipantRepository;
    private final CommunityPostRepository communityPostRepository;

    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("이미 데이터가 존재하여 초기화를 건너뜁니다.");
            createCommunityPosts(userRepository.findAll());
            return;
        }

        log.info("========== 테스트용 데이터 초기화 시작 ==========");

        // 1. 유저 4명 생성
        List<User> users = new ArrayList<>();
        users.add(createTestUser("user1", "경찰1"));
        users.add(createTestUser("user2", "경찰2"));
        users.add(createTestUser("user3", "도둑1"));
        users.add(createTestUser("user4", "도둑2"));

        // 2. 게임 생성 (방장: user1)
        Game game = createGame(users.get(0));

        // 3. 참가자 등록 및 팀 배정 (2:2)
        joinGame(game, users.get(0), Team.POLICE, true);  // 방장 (경찰)
        joinGame(game, users.get(1), Team.POLICE, false); // 경찰
        joinGame(game, users.get(2), Team.ROBBER, false); // 도둑
        joinGame(game, users.get(3), Team.ROBBER, false); // 도둑

        // 4. 게임 시작 상태로 변경
        game.startGame(LocalDateTime.now());
        gameParticipantRepository.updateStatusByGameId(game.getId(), ParticipantStatus.ALIVE);

        log.info("Game Started: [ID: {}, InviteCode: {}]", game.getId(), game.getInviteCode());

        // 5. 커뮤니티 게시글 생성
        createCommunityPosts(users);

        log.info("========== 테스트용 데이터 초기화 완료 ==========");
    }

    private User createTestUser(String socialId, String nickname) {
        User user = User.signUp(socialId, SocialType.GOOGLE, nickname);
        userRepository.save(user);

        UserDevice userDevice = UserDevice.connect(
                user,
                "device-id-" + socialId,
                DeviceType.ANDROID,
                "fcm-token-" + socialId
        );
        userDeviceRepository.save(userDevice);

        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        refreshTokenRepository.save(
                user.getId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );

        log.info("User Created: [ID: {}, Nickname: {}]", user.getId(), nickname);
        log.info("Access Token: {}", accessToken);
        
        return user;
    }

    private Game createGame(User host) {
        GameCreateCommand command = new GameCreateCommand(
                host.getId(),
                new GameAreaData.CircleAreaData(37.5665, 126.978, 1000, 37.5665, 126.978, 100),
                30, 5, 3, 10
        );

        Game game = Game.createGame("TEST12", command);
        gameRepository.save(game);

        GameAreaData.CircleAreaData circleData = (GameAreaData.CircleAreaData) command.areaData();
        Point playgroundCenter = geometryFactory.createPoint(new Coordinate(circleData.playgroundLongitude(), circleData.playgroundLatitude()));
        Point jailCenter = geometryFactory.createPoint(new Coordinate(circleData.jailLongitude(), circleData.jailLatitude()));

        GameArea gameArea = GameArea.createCircleGameArea(
                game,
                playgroundCenter,
                circleData.playgroundRadiusInMeters(),
                jailCenter,
                circleData.jailRadiusInMeters()
        );
        gameAreaRepository.save(gameArea);

        return game;
    }

    private void joinGame(Game game, User user, Team team, boolean isHost) {
        GameParticipant participant = GameParticipant.createParticipant(game, user, isHost);
        participant.changeTeam(team);
        participant.updateReady(true);
        gameParticipantRepository.save(participant);
    }

    /**
     * 커뮤니티 목록 화면 확인용 게시글.
     * 무한스크롤(기본 size 10)이 2페이지로 넘어가도록 12건을 만든다.
     * <p>
     * 주소 조합은 카카오 coord2address 실제 응답 분포를 따랐다. 도로명주소는 건물에 부여되는
     * 체계라 도로명과 건물명이 함께 나오거나 함께 없고, 공원·강·산처럼 건물이 없는 좌표는
     * 지번만 내려온다. 실측(18개 지점) 기준 지번만 나오는 비율이 더 높아 그대로 반영했다.
     */
    private void createCommunityPosts(List<User> users) {
        if (users.isEmpty() || communityPostRepository.count() > 0) {
            return;
        }

        List<CommunityPostSeed> seeds = List.of(
                new CommunityPostSeed("세종대에서 경찰과 도둑 하실 분!", "세종대 정문 앞에서 모입니다. 초보 환영이에요.",
                        37.5502, 127.0736, 8,
                        new PostAddress("서울 광진구 군자동 98", "서울특별시 광진구 능동로 209", "세종대학교")),
                new CommunityPostSeed("강남역 근처 야간 게임", "퇴근하고 가볍게 한 판 하실 분 구합니다.",
                        37.4979, 127.0276, 6,
                        new PostAddress("서울 서초구 서초동 1373", null, null)),
                new CommunityPostSeed("올림픽공원에서 대규모로", "넓은 곳에서 제대로 뛰어봅시다. 20명 목표!",
                        37.5202, 127.1213, 20,
                        new PostAddress("서울 송파구 방이동 88", "서울특별시 송파구 올림픽로 424", "올림픽공원")),
                new CommunityPostSeed("한강공원 뚝섬 모임", "돗자리 깔고 쉬다가 게임도 하고요.",
                        37.5299, 127.0668, 10,
                        new PostAddress("서울 광진구 자양동 704", null, null)),
                new CommunityPostSeed("여의도 벚꽃 시즌 게임", "꽃구경도 하고 게임도 하고 일석이조!",
                        37.5265, 126.9245, 12,
                        new PostAddress("서울 영등포구 여의도동 2", null, null)),
                new CommunityPostSeed("홍대입구 주말 모임", "주말 오후에 홍대에서 만나요.",
                        37.5572, 126.9245, 8,
                        new PostAddress("서울 마포구 동교동 165", null, null)),
                new CommunityPostSeed("잠실 롯데타워 앞", "지방에서 오시는 분들도 찾기 쉬워요.",
                        37.5125, 127.1025, 15,
                        new PostAddress("서울 송파구 신천동 29", "서울특별시 송파구 올림픽로 300", "롯데월드타워")),
                new CommunityPostSeed("북서울꿈의숲 오전 게임", "아침 일찍 시작해서 점심 전에 끝냅니다.",
                        37.6210, 127.0417, 10,
                        new PostAddress("서울 강북구 번동 산28", null, null)),
                new CommunityPostSeed("서울숲에서 만나요", "가족 단위도 환영합니다.",
                        37.5444, 127.0374, 14,
                        new PostAddress("서울 성동구 성수동1가 685", null, null)),
                new CommunityPostSeed("건대입구 저녁 모임", "저녁 먹고 시작할 예정이에요.",
                        37.5405, 127.0700, 6,
                        new PostAddress("서울 광진구 화양동 5", null, null)),
                new CommunityPostSeed("남산공원 야경 게임", "야경 보면서 뛰는 재미가 있습니다.",
                        37.5512, 126.9882, 10,
                        new PostAddress("서울 중구 예장동 산5", null, null)),
                new CommunityPostSeed("주소 변환 실패 케이스", "역지오코딩이 실패하면 주소 3종이 모두 null로 내려갑니다.",
                        37.5665, 126.9780, 6,
                        PostAddress.empty())
        );

        for (int i = 0; i < seeds.size(); i++) {
            CommunityPostSeed seed = seeds.get(i);
            User writer = users.get(i % users.size());
            CommunityPostCreateCommand command = new CommunityPostCreateCommand(
                    writer.getId(),
                    seed.title(),
                    seed.content(),
                    LocalDateTime.now().plusDays(i + 1L),
                    seed.latitude(),
                    seed.longitude(),
                    seed.maxParticipants()
            );
            communityPostRepository.save(CommunityPost.createPost(command, seed.postAddress()));
        }

        log.info("Community Posts Created: [{}건]", seeds.size());
    }

    private record CommunityPostSeed(
            String title,
            String content,
            Double latitude,
            Double longitude,
            Integer maxParticipants,
            PostAddress postAddress
    ) {
    }
}
