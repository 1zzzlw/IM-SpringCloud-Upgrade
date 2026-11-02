package com.zzzlew;

import com.zzzlew.config.NettyConfig;
import com.zzzlew.domain.ClusterMessageWrapper;
import com.zzzlew.domain.Message;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.request.HeartRequestDTO;
import com.zzzlew.domain.request.OnlineStatusListRequestDTO;
import com.zzzlew.domain.response.LoginSuccessResponseVO;
import com.zzzlew.domain.response.OnlineStatusListResponseVO;
import com.zzzlew.domain.response.QuitLoginResponseVO;
import com.zzzlew.handler.HeartBeatHandler;
import com.zzzlew.handler.MessageDispatcherHandler;
import com.zzzlew.handler.messageHandler.OnlineStatusListHandler;
import com.zzzlew.handler.QuitLoginHandler;
import com.zzzlew.listener.MQMessageListener;
import com.zzzlew.publish.MQMessagePublish;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static com.zzzlew.constant.RedisConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PresenceDeliveryTest {
    private final UserBaseDTO user = new UserBaseDTO();
    private final String previousClusterId = NettyConfig.NETTY_CLUSTER_ID;

    PresenceDeliveryTest() {
        user.setId(100L);
        NettyConfig.NETTY_CLUSTER_ID = "presence-test";
    }

    @AfterEach
    void clearChannels() {
        ChannelManageUtil.Online_user.clear();
        ChannelManageUtil.Online_channel.clear();
        NettyConfig.NETTY_CLUSTER_ID = previousClusterId;
    }

    @Test
    void oldDisconnectMustNotRemoveNewConnection() {
        EmbeddedChannel old = new EmbeddedChannel();
        EmbeddedChannel current = new EmbeddedChannel();
        try {
            ChannelManageUtil.addChannel(user, old);
            ChannelManageUtil.addChannel(user, current);
            ChannelManageUtil.removeChannel(old);
            assertSame(current, ChannelManageUtil.getChannel(user.getId()));
            assertNull(ChannelManageUtil.getUser(old));
        } finally {
            old.finishAndReleaseAll();
            current.finishAndReleaseAll();
        }
    }

    private Message presence(int type) {
        if (type == 9) {
            LoginSuccessResponseVO message = new LoginSuccessResponseVO();
            message.setUserId(200L);
            return message;
        }
        QuitLoginResponseVO message = new QuitLoginResponseVO();
        message.setUserId(200L);
        return message;
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 11})
    @SuppressWarnings("unchecked")
    void localPresenceMustQueueWhileWriteBufferIsBusy(int type) {
        EmbeddedChannel channel = new EmbeddedChannel();
        try {
            ChannelManageUtil.addChannel(user, channel);
            channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
            assertFalse(channel.isWritable());
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
            when(redis.opsForHash()).thenReturn(hashes);
            when(hashes.multiGet(USER_CLUSTER_MAPPING_KEY, List.of("100")))
                    .thenReturn(List.of(NettyConfig.NETTY_CLUSTER_ID));
            ExecutorService executor = mock(ExecutorService.class);
            doAnswer(invocation -> {
                invocation.<Runnable>getArgument(0).run();
                return null;
            }).when(executor).execute(any(Runnable.class));
            MQMessagePublish publish = new MQMessagePublish();
            ReflectionTestUtils.setField(publish, "stringRedisTemplate", redis);
            ReflectionTestUtils.setField(publish, "rabbitTemplate", mock(RabbitTemplate.class));
            ReflectionTestUtils.setField(publish, "imAsyncExecutor", executor);
            Message online = presence(type);
            publish.sendToCluster(MessageResult.multiple(online, List.of(100L)));
            channel.runPendingTasks();
            assertSame(online, channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 11})
    void clusterPresenceMustQueueWhileWriteBufferIsBusy(int type) {
        EmbeddedChannel channel = new EmbeddedChannel();
        try {
            ChannelManageUtil.addChannel(user, channel);
            channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
            Message online = presence(type);
            new MQMessageListener().handleClusterMessage(new ClusterMessageWrapper<>(online, 100L));
            assertSame(online, channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void delayedOldCloseMustNotAnnounceOffline() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        MQMessagePublish publish = mock(MQMessagePublish.class);
        QuitLoginHandler handler = new QuitLoginHandler();
        ReflectionTestUtils.setField(handler, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(handler, "mqMessagePublish", publish);
        EmbeddedChannel old = new EmbeddedChannel(handler);
        EmbeddedChannel current = new EmbeddedChannel();
        try {
            ChannelManageUtil.addChannel(user, old);
            ChannelManageUtil.addChannel(user, current);
            old.finishAndReleaseAll();
            assertSame(current, ChannelManageUtil.getChannel(100L));
            verifyNoInteractions(publish);
        } finally {
            old.finishAndReleaseAll();
            current.finishAndReleaseAll();
        }
    }

    @Test
    void connectionClosedBeforeAuthenticationMustNotThrow() {
        QuitLoginHandler handler = new QuitLoginHandler();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ReflectionTestUtils.setField(handler, "stringRedisTemplate", redis);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        assertDoesNotThrow(() -> channel.finishAndReleaseAll());
        verifyNoInteractions(redis);
    }

    @Test
    @SuppressWarnings("unchecked")
    void explicitPresenceRequestReturnsCurrentOnlineFriends() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SetOperations<String, String> sets = mock(SetOperations.class);
        when(redis.opsForSet()).thenReturn(sets);
        when(sets.intersect(USER_ONLINE_STATUS_KEY, USER_FRIEND_LIST_KEY + "100"))
                .thenReturn(Set.of("200"));
        MessageDispatcherHandler dispatcher = new MessageDispatcherHandler();
        MQMessagePublish publish = mock(MQMessagePublish.class);
        ReflectionTestUtils.setField(dispatcher, "mqMessagePublish", publish);
        java.util.Map<Integer, Object> handlers = (java.util.Map<Integer, Object>)
                ReflectionTestUtils.getField(dispatcher, "handlerMap");
        OnlineStatusListHandler handler = (OnlineStatusListHandler)
                handlers.get(Message.OnlineStatusListRequestDTO);
        ReflectionTestUtils.setField(handler, "stringRedisTemplate", redis);
        EmbeddedChannel channel = new EmbeddedChannel(dispatcher);
        try {
            ChannelManageUtil.addChannel(user, channel);
            channel.writeInbound(new OnlineStatusListRequestDTO());
            OnlineStatusListResponseVO snapshot = channel.readOutbound();
            assertNotNull(snapshot);
            assertEquals(List.of("200"), snapshot.getFriendIdList());
            when(sets.intersect(USER_ONLINE_STATUS_KEY, USER_FRIEND_LIST_KEY + "100"))
                    .thenReturn(Set.of());
            channel.writeInbound(new OnlineStatusListRequestDTO());
            snapshot = channel.readOutbound();
            assertEquals(List.of(), snapshot.getFriendIdList());
            verifyNoInteractions(publish);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void heartbeatOnlyAcknowledgesLivenessAndDoesNotQueryFriendList() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HeartBeatHandler handler = new HeartBeatHandler();
        ReflectionTestUtils.setField(handler, "stringRedisTemplate", redis);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        try {
            ChannelManageUtil.addChannel(user, channel);
            HeartRequestDTO heartbeat = new HeartRequestDTO();
            heartbeat.setSequenceId(37);
            channel.writeInbound(heartbeat);
            Message pong = channel.readOutbound();
            assertEquals(0, pong.getMessageType());
            assertEquals(37, pong.getSequenceId());
            assertNull(channel.readOutbound());
            verify(redis, never()).opsForSet();
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
