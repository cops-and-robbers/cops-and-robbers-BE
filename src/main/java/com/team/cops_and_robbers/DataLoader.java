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


        for (int i = 0; i < COMMUNITY_POST_COUNT; i++) {
            CommunityPostSeed seed = COMMUNITY_POST_SEEDS.get(i % COMMUNITY_POST_SEEDS.size());
            User writer = users.get(i % users.size());
            CommunityPostCreateCommand command = new CommunityPostCreateCommand(
                    writer.getId(),
                    toTitle(seed, i, COMMUNITY_POST_SEEDS.size()),
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

    /** dev 확인용 시드. 주소는 실제 벤더 응답을 그대로 옮긴 값이다. */
    private static final List<CommunityPostSeed> COMMUNITY_POST_SEEDS = List.of(
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
                new CommunityPostSeed("吉祥寺でケイドロやりませんか", "夕方から軽く一回！初心者歓迎です。",
                        35.7031, 139.5797, 8,
                        "吉祥寺駅 北口",
                        PostAddress.of("吉祥寺, 武蔵野市, TK 180-0005, 日本",
                                null, null, "東京 武蔵野市", "JP")),
                new CommunityPostSeed("舞鶴で第2回やります", "前回楽しかったのでまた集まります。",
                        35.4751, 135.386, 6,
                        "舞鶴赤れんがパーク",
                        PostAddress.of("舞鶴市役所, 国道27号, 北吸, 舞鶴市, 625-0087, 日本",
                                "国道27号", null, "舞鶴市 北吸", "JP")),
                new CommunityPostSeed("渋谷スクランブル 夜の部", "人が多いところでスリル満点です。",
                        35.6595, 139.7005, 10,
                        "渋谷スクランブル交差点",
                        PostAddress.of("2-9 道玄坂二丁目, 東京, TK 150-0043, 日本",
                                "2-9 道玄坂二丁目", null, "東京", "JP")),
                new CommunityPostSeed("新宿御苑で午前中に", "広い公園なので思いきり走れます。",
                        35.6852, 139.71, 12,
                        "新宿御苑 大木戸門",
                        PostAddress.of("中央休憩所売店, 甲州街道, 新宿区, TK 160-8484, 日本",
                                "甲州街道", null, "東京 新宿区", "JP")),
                new CommunityPostSeed("上野公園 週末集合", "家族連れも大歓迎です。",
                        35.7148, 139.7737, 10,
                        "上野公園 噴水前",
                        PostAddress.of("正岡子規記念球場, パンダ橋, 上野公園, 台東区, TK 110-0007, 日本",
                                "パンダ橋", null, "東京 台東区 上野公園", "JP")),
                new CommunityPostSeed("大阪城公園で大人数", "20人目標で大きくやりましょう！",
                        34.6873, 135.5262, 20,
                        "大阪城公園 太陽の広場",
                        PostAddress.of("大阪城, 1, 中央区, 大阪市, 大阪城 540-0002, 日本",
                                null, null, "大阪市 中央区", "JP")),
                new CommunityPostSeed("横浜みなとみらい 夜景ラン", "夜景を見ながら走れます。",
                        35.456, 139.638, 8,
                        "横浜赤レンガ倉庫",
                        PostAddress.of("7-9 新港二丁目, 横浜市, KN 231-0063, 日本",
                                "7-9 新港二丁目", null, "神奈川 横浜市", "JP")),
                new CommunityPostSeed("札幌大通公園でナイトゲーム", "涼しい時間帯にやります。",
                        43.059, 141.3468, 10,
                        "大通公園 テレビ塔前",
                        PostAddress.of("1-12 大通西七丁目, 札幌, HK 064-0801, 日本",
                                "1-12 大通西七丁目", null, "北海 札幌", "JP")),
                new CommunityPostSeed("Cops and Robbers at Central Park", "Casual game after work. Beginners welcome!",
                        40.7829, -73.9654, 10,
                        "Bethesda Terrace",
                        PostAddress.of("Great Lawn Ball Field 8, 86th Street Transverse, New York, NY 11025, United States of America",
                                "86th Street Transverse", null, "New York Manhattan", "US")),
                new CommunityPostSeed("Hyde Park weekend run", "Sunday afternoon, see you there.",
                        51.5073, -0.1657, 8,
                        "Speakers' Corner",
                        PostAddress.of("Hyde Park, London, ENG, United Kingdom",
                                null, null, "England London Hyde Park", "GB")),
                new CommunityPostSeed("Darling Harbour sunset game", "Starting right before sunset.",
                        -33.8737, 151.2004, 8,
                        "Darling Harbour Playground",
                        PostAddress.of("5110 Western Distributor, Sydney NSW 2000, Australia",
                                "5110 Western Distributor", null, "New South Wales Sydney", "AU")),
                new CommunityPostSeed("남산공원 야경 게임", "야경 보면서 뛰는 재미가 있습니다.",
                        37.5512, 126.9882, 10,
                        "남산공원 백범광장",
                        PostAddress.of("서울특별시 용산구 용산동2가 산 1-11", null, null,
                                "서울특별시 용산구 용산동2가", "KR"))
    );

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
