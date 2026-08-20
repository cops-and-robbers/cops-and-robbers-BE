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

    /** 커서 페이지네이션이 여러 페이지로 넘어가는지 확인할 수 있도록 넉넉히 만든다. 기본 size가 10이라 5페이지가 된다. */
    private static final int COMMUNITY_POST_COUNT = 50;

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
            log.info("이미 데이터가 존재하여 초기화를 건너뜁니다. 커뮤니티 게시글이 없으면 그것만 생성합니다.");
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
     * 커뮤니티 목록 화면 확인용 게시글. 시드를 순환해 {@link #COMMUNITY_POST_COUNT}건을 만든다.
     * <p>
     * 주소는 VWorld·Geoapify를 실제로 호출해 받은 값이다. 국내는 12개 지점 중 롯데타워만 도로명·건물명이
     * 나오고 나머지는 지번만 내려왔다. 실제 분포가 그러하므로 시드도 그대로 맞춘다.
     */
    private void createCommunityPosts(List<User> users) {
        if (users.isEmpty() || communityPostRepository.count() > 0) {
            return;
        }

        List<CommunityPostSeed> seeds = List.of(
                new CommunityPostSeed("잠실 롯데타워 앞", "지방에서 오시는 분들도 찾기 쉬워요.",
                        37.5125, 127.1025, 15,
                        "롯데월드타워 정문",
                        PostAddress.of("서울특별시 송파구 신천동 29",
                                "서울특별시 송파구 올림픽로 300", "롯데월드타워앤드롯데월드몰",
                                "서울특별시 송파구 신천동", "KR")),
                new CommunityPostSeed("세종대에서 경찰과 도둑 하실 분!", "세종대 정문 앞에서 모입니다. 초보 환영이에요.",
                        37.5502, 127.0736, 8,
                        "세종대 정문",
                        PostAddress.of("서울특별시 광진구 군자동 98", null, null,
                                "서울특별시 광진구 군자동", "KR")),
                new CommunityPostSeed("강남역 근처 야간 게임", "퇴근하고 가볍게 한 판 하실 분 구합니다.",
                        37.4979, 127.0276, 6,
                        "강남역 11번 출구",
                        PostAddress.of("서울특별시 서초구 서초동 1373", null, null,
                                "서울특별시 서초구 서초동", "KR")),
                new CommunityPostSeed("올림픽공원에서 대규모로", "넓은 곳에서 제대로 뛰어봅시다. 20명 목표!",
                        37.5202, 127.1213, 20,
                        "올림픽공원 평화의문",
                        PostAddress.of("서울특별시 송파구 방이동 88-3", null, null,
                                "서울특별시 송파구 방이동", "KR")),
                new CommunityPostSeed("한강공원 뚝섬 모임", "돗자리 깔고 쉬다가 게임도 하고요.",
                        37.5299, 127.0668, 10,
                        "뚝섬한강공원 자벌레",
                        PostAddress.of("서울특별시 광진구 자양동 112", null, null,
                                "서울특별시 광진구 자양동", "KR")),
                new CommunityPostSeed("여의도 벚꽃 시즌 게임", "꽃구경도 하고 게임도 하고 일석이조!",
                        37.5265, 126.9245, 12,
                        "여의도공원 문화의마당",
                        PostAddress.of("서울특별시 영등포구 여의도동 2", null, null,
                                "서울특별시 영등포구 여의도동", "KR")),
                new CommunityPostSeed("홍대입구 주말 모임", "주말 오후에 홍대에서 만나요.",
                        37.5572, 126.9245, 8,
                        "홍대입구역 9번 출구",
                        PostAddress.of("서울특별시 마포구 동교동 155-55", null, null,
                                "서울특별시 마포구 동교동", "KR")),
                new CommunityPostSeed("북서울꿈의숲 오전 게임", "아침 일찍 시작해서 점심 전에 끝냅니다.",
                        37.6210, 127.0417, 10,
                        "북서울꿈의숲 전망대",
                        PostAddress.of("서울특별시 강북구 번동 90", null, null,
                                "서울특별시 강북구 번동", "KR")),
                new CommunityPostSeed("서울숲에서 만나요", "가족 단위도 환영합니다.",
                        37.5444, 127.0374, 14,
                        "서울숲 가족마당",
                        PostAddress.of("서울특별시 성동구 성수동1가 720", null, null,
                                "서울특별시 성동구 성수동1가", "KR")),
                new CommunityPostSeed("건대입구 저녁 모임", "저녁 먹고 시작할 예정이에요.",
                        37.5405, 127.0700, 6,
                        "건대입구역 2번 출구",
                        PostAddress.of("서울특별시 광진구 화양동 6-7", null, null,
                                "서울특별시 광진구 화양동", "KR")),
                new CommunityPostSeed("남산공원 야경 게임", "야경 보면서 뛰는 재미가 있습니다.",
                        37.5512, 126.9882, 10,
                        "남산공원 백범광장",
                        PostAddress.of("서울특별시 용산구 용산동2가 산 1-11", null, null,
                                "서울특별시 용산구 용산동2가", "KR")),
                new CommunityPostSeed("뉴욕 타임스퀘어 번개", "해외 좌표는 Geoapify가 변환합니다.",
                        40.7580, -73.9855, 8,
                        "타임스퀘어",
                        PostAddress.of("Sunglass Hut, 1540 Broadway, New York, NY 10036, United States of America",
                                "1540 Broadway", null, "New York Manhattan", "US")),
                new CommunityPostSeed("런던 빅벤 앞에서", "해외 게시글 표시 확인용입니다.",
                        51.5007, -0.1246, 6,
                        "빅벤 앞",
                        PostAddress.of("Elizabeth Tower, Bridge Street, London, SW1A 2JR, United Kingdom",
                                "Bridge Street", null, "England City of Westminster Millbank", "GB")),
                new CommunityPostSeed("기치조지에서 경찰과 도둑", "일본 좌표는 현지어로 저장됩니다.",
                        35.7022, 139.5803, 8,
                        "吉祥寺駅 北口",
                        PostAddress.of("吉祥寺大通り, 吉祥寺本町, 武蔵野市, TK 180-0000, 日本",
                                null, null, "東京 武蔵野市 吉祥寺本町", "JP")),
                new CommunityPostSeed("타이베이 101 앞", "지원 언어가 아닌 국가는 영어로 저장됩니다.",
                        25.0330, 121.5654, 6,
                        "台北101 入口",
                        PostAddress.of("１００號 信義路五段, Xicun, Taipei 110615, Taiwan",
                                "１００號 信義路五段", null, "Taipei Xicun", "TW"))
        );

        for (int i = 0; i < COMMUNITY_POST_COUNT; i++) {
            CommunityPostSeed seed = seeds.get(i % seeds.size());
            User writer = users.get(i % users.size());
            CommunityPostCreateCommand command = new CommunityPostCreateCommand(
                    writer.getId(),
                    toTitle(seed, i, seeds.size()),
                    seed.content(),
                    LocalDateTime.now().plusDays(i + 1L),
                    seed.latitude(),
                    seed.longitude(),
                    seed.placeName(),
                    seed.maxParticipants()
            );
            communityPostRepository.save(CommunityPost.createPost(command, seed.postAddress()));
        }

        log.info("Community Posts Created: [{}건]", COMMUNITY_POST_COUNT);
    }

    /** 시드를 순환해 만들기 때문에 두 바퀴째부터는 제목에 회차를 붙여 목록에서 구분되게 한다. */
    private String toTitle(CommunityPostSeed seed, int index, int seedCount) {
        int round = index / seedCount;
        if (round == 0) {
            return seed.title();
        }
        return seed.title() + " (" + (round + 1) + "차)";
    }

    private record CommunityPostSeed(
            String title,
            String content,
            Double latitude,
            Double longitude,
            Integer maxParticipants,
            String placeName,
            PostAddress postAddress
    ) {
    }
}
