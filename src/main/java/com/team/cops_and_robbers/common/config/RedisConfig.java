package com.team.cops_and_robbers.common.config;

import com.team.cops_and_robbers.common.constant.RedisChannel;
import com.team.cops_and_robbers.common.constant.RedisProperties;
import com.team.cops_and_robbers.play.chat.application.AllChatSubscriber;
import com.team.cops_and_robbers.play.chat.application.PoliceChatSubscriber;
import com.team.cops_and_robbers.play.chat.application.RobberChatSubscriber;
import com.team.cops_and_robbers.play.lobby.application.LobbySubscriber;
import com.team.cops_and_robbers.play.location.application.PoliceLocationSubscriber;
import com.team.cops_and_robbers.play.location.application.RobberLocationSubscriber;
import com.team.cops_and_robbers.play.system.application.SystemSubscriber;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {

    private static final String LISTENER_METHOD_NAME = "onMessage";

    private final RedisProperties redisProperties;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = redisProperties.getRedissonAddress();
        SingleServerConfig serverConfig = config.useSingleServer().setAddress(address);
        String password = redisProperties.password();

        if (password != null && !password.isBlank()) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(RedissonClient redissonClient) {
        return new RedissonConnectionFactory(redissonClient);
    }

    /**
     * 1. StringRedisTemplate
     * 용도: 리프레시 토큰 (문자열 데이터)
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        StringRedisTemplate stringRedisTemplate = new StringRedisTemplate();
        stringRedisTemplate.setConnectionFactory(redisConnectionFactory);
        return stringRedisTemplate;
    }

    /**
     * 2. RedisTemplate<String, Object>
     * 용도: 단순 문자열이 아닌 객체를 저장 (json)
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));

        return redisTemplate;
    }

    /**
     * 3. RedisMessageListenerContainer
     * 용도: Redis Pub/Sub 메시지의 통로
     * 메세지를 처리할 리스너(subscriber) 등록
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            Map<String, MessageListenerAdapter> listenerMappings
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        listenerMappings.forEach((pattern, adapter) ->
            container.addMessageListener(adapter, new PatternTopic(pattern))
        );
        return container;
    }

    /**
     * 4. Redis 토픽 패턴과 실제 리스너(subscriber) 매핑 객체 생성
     */
    @Bean
    public Map<String, MessageListenerAdapter> listenerMappings(
            MessageListenerAdapter lobbyListenerAdapter,
            MessageListenerAdapter systemListenerAdapter,
            MessageListenerAdapter allChatAdapter,
            MessageListenerAdapter policeChatAdapter,
            MessageListenerAdapter robberChatAdapter,
            MessageListenerAdapter policeLocationAdapter,
            MessageListenerAdapter robberLocationAdapter
    ) {
        return Map.of(
                RedisChannel.LOBBY.getPattern(), lobbyListenerAdapter,
                RedisChannel.SYSTEM.getPattern(), systemListenerAdapter,
                RedisChannel.CHAT_ALL.getPattern(), allChatAdapter,
                RedisChannel.CHAT_POLICE.getPattern(), policeChatAdapter,
                RedisChannel.CHAT_ROBBER.getPattern(), robberChatAdapter,
                RedisChannel.LOCATION_POLICE.getPattern(), policeLocationAdapter,
                RedisChannel.LOCATION_ROBBER.getPattern(), robberLocationAdapter
        );
    }

    /**
     * 5. MessageListenerAdapter
     * 용도: 각 도메인 별로 메시지가 오면 'xxxSubscriber' 서비스의 'onMessage' 메소드를 실행하도록 연결
     */
    @Bean
    public MessageListenerAdapter lobbyListenerAdapter(LobbySubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD_NAME);
    }

    @Bean
    public MessageListenerAdapter systemListenerAdapter(SystemSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD_NAME);
    }

    @Bean
    public MessageListenerAdapter allChatAdapter(AllChatSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD_NAME);
    }

    @Bean
    public MessageListenerAdapter policeChatAdapter(PoliceChatSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD_NAME);
    }

    @Bean
    public MessageListenerAdapter robberChatAdapter(RobberChatSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD_NAME);
    }

    @Bean
    public MessageListenerAdapter policeLocationAdapter(PoliceLocationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD_NAME);
    }

    @Bean
    public MessageListenerAdapter robberLocationAdapter(RobberLocationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, LISTENER_METHOD_NAME);
    }
}
