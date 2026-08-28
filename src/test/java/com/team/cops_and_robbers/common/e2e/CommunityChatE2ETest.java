package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityChatGameInviteData;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityChatPayload;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.community.presentation.dto.request.CommunityChatRequest;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@DisplayName("커뮤니티 채팅 E2E")
class CommunityChatE2ETest extends WebSocketE2ETest {

    private static final String CHAT_CHANNEL = "/subscribe/community/%d/chat";
    private static final String USER_CHANNEL = "/subscribe/user/%d/community/chat";

    private record ChatSetup(
            CommunityPost post,
            String chatChannel,
            StompTestClient authorClient
    ) {}

    private ChatSetup givenChatRoomWithSubscribedAuthor(User author) throws Exception {
        CommunityPost post = communityPostRepository.save(CommunityPostFixture.POST(author.getId()));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), author.getId()));

        String chatChannel = CHAT_CHANNEL.formatted(post.getId());
        StompTestClient authorClient = connect(givenAccessToken(author));
        authorClient.subscribe(chatChannel, CommunityChatPayload.class);

        return new ChatSetup(post, chatChannel, authorClient);
    }

    @Test
    void 유저가_채팅방에_참여하면_JOIN_시스템_메시지를_수신한다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        User joiner = givenUser("joiner");
        String joinerToken = givenAccessToken(joiner);

        authenticated(joinerToken)
                .post("/api/community-posts/{postId}/chat/join", setup.post().getId())
                .then()
                .statusCode(201);

        CommunityChatPayload received = setup.authorClient().waitForMessage(setup.chatChannel(), 5);

        assertThat(received.messageType()).isEqualTo(CommunityChatMessageType.SYSTEM);
        assertThat(received.message()).isEqualTo("{\"event\":\"JOIN\"}");
        assertThat(received.senderId()).isEqualTo(joiner.getId());
        assertThat(received.senderNickname()).isEqualTo(joiner.getNickname());
        assertThat(received.senderProfileIcon()).isEqualTo(joiner.getProfileIcon());
        assertThat(received.id()).isNotNull();
        assertThat(received.communityPostId()).isEqualTo(setup.post().getId());
    }

    @Test
    void 유저가_채팅방에서_나가면_LEAVE_시스템_메시지를_수신한다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        User leaver = givenUser("leaver");
        communityChatMemberRepository.save(CommunityChatMember.createMember(setup.post().getId(), leaver.getId()));

        authenticated(givenAccessToken(leaver))
                .delete("/api/community-posts/{postId}/chat/leave", setup.post().getId())
                .then()
                .statusCode(204);

        CommunityChatPayload received = setup.authorClient().waitForMessage(setup.chatChannel(), 5);

        assertThat(received.messageType()).isEqualTo(CommunityChatMessageType.SYSTEM);
        assertThat(received.message()).isEqualTo("{\"event\":\"LEAVE\"}");
        assertThat(received.senderId()).isEqualTo(leaver.getId());
    }

    @Test
    void 멤버가_보낸_메시지를_같은_방_구독자가_수신한다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        User sender = givenUser("sender");
        communityChatMemberRepository.save(CommunityChatMember.createMember(setup.post().getId(), sender.getId()));
        StompTestClient senderClient = connect(givenAccessToken(sender));
        senderClient.subscribe(setup.chatChannel(), CommunityChatPayload.class);

        senderClient.send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest("key-1", "안녕하세요!", null, CommunityChatMessageType.TEXT)
        );

        CommunityChatPayload received = setup.authorClient().waitForMessage(setup.chatChannel(), 5);

        assertThat(received.messageType()).isEqualTo(CommunityChatMessageType.TEXT);
        assertThat(received.message()).isEqualTo("안녕하세요!");
        assertThat(received.messageKey()).isEqualTo("key-1");
        assertThat(received.senderId()).isEqualTo(sender.getId());
        assertThat(received.senderNickname()).isEqualTo(sender.getNickname());
        assertThat(received.senderProfileIcon()).isEqualTo(sender.getProfileIcon());
        assertThat(received.id()).as("즉시 INSERT라 발행 시점에 id가 확정된다").isNotNull();
    }

    @Test
    void 게임_초대는_서버가_JSON으로_직렬화해_저장한다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        setup.authorClient().send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest(
                        "key-1",
                        null,
                        new CommunityChatGameInviteData("ABC123"),
                        CommunityChatMessageType.GAME_INVITE)
        );

        CommunityChatPayload received = setup.authorClient().waitForMessage(setup.chatChannel(), 5);

        assertThat(received.messageType()).isEqualTo(CommunityChatMessageType.GAME_INVITE);
        assertThat(received.message()).contains("\"inviteCode\":\"ABC123\"");
        assertThat(received.senderNickname()).isEqualTo(author.getNickname());
    }

    @Test
    void 초대_코드가_없는_게임_초대는_저장되지_않는다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        setup.authorClient().send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest(
                        "key-1",
                        null,
                        new CommunityChatGameInviteData(null),
                        CommunityChatMessageType.GAME_INVITE)
        );

        assertThat(setup.authorClient().<CommunityChatPayload>waitForMessage(setup.chatChannel(), 1)).isNull();
        assertThat(communityChatMessageRepository.count()).isZero();
    }

    @Test
    void 컬럼_길이를_넘는_메시지_키는_저장되지_않는다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        setup.authorClient().send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest("k".repeat(37), "안녕하세요!", null, CommunityChatMessageType.TEXT)
        );

        assertThat(setup.authorClient().<CommunityChatPayload>waitForMessage(setup.chatChannel(), 1)).isNull();
        assertThat(communityChatMessageRepository.count()).isZero();
    }

    @Test
    void 메시지_키를_보내지_않으면_서버가_채운다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        setup.authorClient().send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest(null, "안녕하세요!", null, CommunityChatMessageType.TEXT)
        );

        CommunityChatPayload received = setup.authorClient().waitForMessage(setup.chatChannel(), 5);

        assertThat(received.messageKey()).isNotBlank();
    }

    @Test
    void 클라이언트는_SYSTEM_타입을_보낼_수_없다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        setup.authorClient().send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest("key-1", "{\"event\":\"JOIN\"}", null, CommunityChatMessageType.SYSTEM)
        );

        assertThat(setup.authorClient().<CommunityChatPayload>waitForMessage(setup.chatChannel(), 1)).isNull();
        assertThat(communityChatMessageRepository.count()).isZero();
    }

    @Test
    void 채팅방_멤버가_아니면_구독이_거부되어_메시지를_받지_못한다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);

        User outsider = givenUser("outsider");
        StompTestClient outsiderClient = connect(givenAccessToken(outsider));
        outsiderClient.subscribe(setup.chatChannel(), CommunityChatPayload.class);

        User joiner = givenUser("joiner");
        authenticated(givenAccessToken(joiner))
                .post("/api/community-posts/{postId}/chat/join", setup.post().getId())
                .then()
                .statusCode(201);

        assertThat(setup.authorClient().<CommunityChatPayload>waitForMessage(setup.chatChannel(), 5))
                .as("멤버인 작성자는 수신한다")
                .isNotNull();
        assertThat(outsiderClient.<CommunityChatPayload>waitForMessage(setup.chatChannel(), 1))
                .as("비멤버는 구독이 거부되어 수신하지 못한다")
                .isNull();
    }

    @Test
    void 목록_채널로_내가_속한_방의_메시지를_받는다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);
        String userChannel = USER_CHANNEL.formatted(author.getId());
        setup.authorClient().subscribe(userChannel, CommunityChatPayload.class);

        User sender = givenUser("sender");
        communityChatMemberRepository.save(CommunityChatMember.createMember(setup.post().getId(), sender.getId()));
        StompTestClient senderClient = connect(givenAccessToken(sender));

        senderClient.send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest("key-1", "안녕하세요!", null, CommunityChatMessageType.TEXT)
        );

        CommunityChatPayload received = setup.authorClient().waitForMessage(userChannel, 5);

        assertThat(received.communityPostId()).isEqualTo(setup.post().getId());
        assertThat(received.message()).isEqualTo("안녕하세요!");
        assertThat(received.senderId()).isEqualTo(sender.getId());
        assertThat(received.senderProfileIcon()).isEqualTo(sender.getProfileIcon());
    }

    @Test
    void 다른_유저의_목록_채널은_구독할_수_없다() throws Exception {
        User author = givenUser("author");
        ChatSetup setup = givenChatRoomWithSubscribedAuthor(author);
        String authorChannel = USER_CHANNEL.formatted(author.getId());
        setup.authorClient().subscribe(authorChannel, CommunityChatPayload.class);

        User stranger = givenUser("stranger");
        StompTestClient strangerClient = connect(givenAccessToken(stranger));
        strangerClient.subscribe(authorChannel, CommunityChatPayload.class);

        setup.authorClient().send(
                "/publish/community/%d/chat".formatted(setup.post().getId()),
                new CommunityChatRequest("key-1", "안녕하세요!", null, CommunityChatMessageType.TEXT)
        );

        assertThat(setup.authorClient().<CommunityChatPayload>waitForMessage(authorChannel, 5))
                .as("채널 주인은 수신한다")
                .isNotNull();
        assertThat(strangerClient.<CommunityChatPayload>waitForMessage(authorChannel, 1))
                .as("남의 채널은 구독이 거부되어 수신하지 못한다")
                .isNull();
        assertThat(strangerClient.isConnected())
                .as("구독만 거부하고 연결은 끊지 않는다")
                .isTrue();
    }
}
