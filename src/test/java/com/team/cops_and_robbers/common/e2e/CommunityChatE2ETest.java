package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityChatMessageType;
import com.team.cops_and_robbers.community.domain.CommunityChatPayload;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("e2e")
@DisplayName("커뮤니티 채팅 E2E")
class CommunityChatE2ETest extends WebSocketE2ETest {

    private static final String CHAT_CHANNEL = "/subscribe/community/%d/chat";

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
}
