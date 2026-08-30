package com.team.cops_and_robbers.common.e2e;

import com.team.cops_and_robbers.common.StompTestClient;
import com.team.cops_and_robbers.common.WebSocketE2ETest;
import com.team.cops_and_robbers.common.fixture.CommunityPostFixture;
import com.team.cops_and_robbers.community.domain.CommunityChatMember;
import com.team.cops_and_robbers.community.domain.CommunityChatPinPayload;
import com.team.cops_and_robbers.community.domain.CommunityChatSystemEventType;
import com.team.cops_and_robbers.community.domain.CommunityPost;
import com.team.cops_and_robbers.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("e2e")
@DisplayName("커뮤니티 채팅 고정 채팅 E2E")
class CommunityChatPinE2ETest extends WebSocketE2ETest {

    private static final String PIN_CHANNEL = "/subscribe/community/%d/chat/pin";
    private static final String PIN_PATH = "/api/community-posts/{postId}/chat/pin";

    private record PinSetup(
            CommunityPost post,
            String pinChannel,
            StompTestClient memberClient
    ) {}

    private PinSetup givenChatRoomWithSubscribedMember(User writer, User member) throws Exception {
        CommunityPost post = communityPostRepository.save(CommunityPostFixture.POST(writer.getId()));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), writer.getId()));
        communityChatMemberRepository.save(CommunityChatMember.createMember(post.getId(), member.getId()));

        String pinChannel = PIN_CHANNEL.formatted(post.getId());
        StompTestClient memberClient = connect(givenAccessToken(member));
        memberClient.subscribe(pinChannel, CommunityChatPinPayload.class);

        return new PinSetup(post, pinChannel, memberClient);
    }

    @Test
    void 방장이_고정_채팅을_등록하면_방_멤버가_실시간으로_수신한다() throws Exception {
        User writer = givenUser("author");
        User member = givenUser("member");
        PinSetup setup = givenChatRoomWithSubscribedMember(writer, member);

        authenticated(givenAccessToken(writer))
                .body(Map.of("content", "정문에서 만나요"))
                .post(PIN_PATH, setup.post().getId())
                .then().statusCode(201);

        CommunityChatPinPayload received = setup.memberClient().waitForMessage(setup.pinChannel(), 5);

        assertSoftly(softly -> {
            softly.assertThat(received.postId()).isEqualTo(setup.post().getId());
            softly.assertThat(received.action()).isEqualTo(CommunityChatSystemEventType.PIN_REGISTERED);
            softly.assertThat(received.content()).isEqualTo("정문에서 만나요");
            softly.assertThat(received.writerId()).isEqualTo(writer.getId());
        });
    }

    @Test
    void 방장이_고정_채팅을_수정하면_방_멤버가_바뀐_내용을_수신한다() throws Exception {
        User writer = givenUser("author");
        User member = givenUser("member");
        PinSetup setup = givenChatRoomWithSubscribedMember(writer, member);
        authenticated(givenAccessToken(writer))
                .body(Map.of("content", "예전 공지"))
                .post(PIN_PATH, setup.post().getId())
                .then().statusCode(201);
        setup.memberClient().waitForMessage(setup.pinChannel(), 5);

        authenticated(givenAccessToken(writer))
                .body(Map.of("content", "바뀐 공지"))
                .put(PIN_PATH, setup.post().getId())
                .then().statusCode(200);

        CommunityChatPinPayload received = setup.memberClient().waitForMessage(setup.pinChannel(), 5);

        assertSoftly(softly -> {
            softly.assertThat(received.action()).isEqualTo(CommunityChatSystemEventType.PIN_UPDATED);
            softly.assertThat(received.content()).isEqualTo("바뀐 공지");
        });
    }

    @Test
    void 방장이_고정_채팅을_삭제하면_방_멤버가_삭제_알림을_수신한다() throws Exception {
        User writer = givenUser("author");
        User member = givenUser("member");
        PinSetup setup = givenChatRoomWithSubscribedMember(writer, member);
        authenticated(givenAccessToken(writer))
                .body(Map.of("content", "정문에서 만나요"))
                .post(PIN_PATH, setup.post().getId())
                .then().statusCode(201);
        setup.memberClient().waitForMessage(setup.pinChannel(), 5);

        authenticated(givenAccessToken(writer))
                .delete(PIN_PATH, setup.post().getId())
                .then().statusCode(204);

        CommunityChatPinPayload received = setup.memberClient().waitForMessage(setup.pinChannel(), 5);

        assertSoftly(softly -> {
            softly.assertThat(received.action()).isEqualTo(CommunityChatSystemEventType.PIN_DELETED);
            softly.assertThat(received.content()).isNull();
        });
    }

    @Test
    void 채팅방_멤버가_아니면_고정_채팅_채널_구독이_거부된다() throws Exception {
        User writer = givenUser("author");
        User member = givenUser("member");
        PinSetup setup = givenChatRoomWithSubscribedMember(writer, member);

        User outsider = givenUser("outsider");
        StompTestClient outsiderClient = connect(givenAccessToken(outsider));
        outsiderClient.subscribe(setup.pinChannel(), CommunityChatPinPayload.class);

        authenticated(givenAccessToken(writer))
                .body(Map.of("content", "정문에서 만나요"))
                .post(PIN_PATH, setup.post().getId())
                .then().statusCode(201);

        assertThat(setup.memberClient().<CommunityChatPinPayload>waitForMessage(setup.pinChannel(), 5))
                .as("멤버는 수신한다")
                .isNotNull();
        assertThat(outsiderClient.<CommunityChatPinPayload>waitForMessage(setup.pinChannel(), 1))
                .as("비멤버는 구독이 거부되어 수신하지 못한다")
                .isNull();
    }
}
