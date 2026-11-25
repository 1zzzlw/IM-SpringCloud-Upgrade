package com.zzzlew;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zzzlew.config.NettyConfig;
import com.zzzlew.domain.ClusterMessageWrapper;
import com.zzzlew.domain.Message;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.request.HeartRequestDTO;
import com.zzzlew.domain.request.OnlineStatusListRequestDTO;
import com.zzzlew.domain.response.DevicePresenceResponseVO;
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
import io.netty.channel.DefaultChannelId;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
        ChannelManageUtil.getChannels(user.getId()).forEach(ChannelManageUtil::removeChannel);
        NettyConfig.NETTY_CLUSTER_ID = previousClusterId;
    }

    private void addChannel(EmbeddedChannel channel) {
        addChannel(channel, LEGACY_DEVICE_ID);
    }

    private void addChannel(EmbeddedChannel channel, String deviceId) {
        ChannelManageUtil.authenticate(channel, user, LEGACY_DEVICE_ID.equals(deviceId) ? null : deviceId,
                "access-" + deviceId);
        ChannelManageUtil.register(channel);
    }

    @Test
    void oldDisconnectMustNotRemoveNewConnection() {
        EmbeddedChannel old = new EmbeddedChannel();
        EmbeddedChannel current = new EmbeddedChannel();
        try {
            addChannel(old);
            addChannel(current);
            ChannelManageUtil.removeChannel(old);
            assertEquals(List.of(current), ChannelManageUtil.getChannels(user.getId()));
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
            addChannel(channel);
            channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
            assertFalse(channel.isWritable());
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
            when(redis.opsForHash()).thenReturn(hashes);
            when(hashes.multiGet(USER_CLUSTER_MAPPING_KEY, List.of("100")))
                    .thenReturn(List.of(JSON.toJSONString(Map.of(LEGACY_DEVICE_ID,
                            List.of(NettyConfig.NETTY_CLUSTER_ID, channel.id().asLongText())))));
            when(redis.hasKey(LOGIN_USERINFO_ACCESSTOKEN_KEY + "access-" + LEGACY_DEVICE_ID)).thenReturn(true);
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
            addChannel(channel);
            channel.unsafe().outboundBuffer().setUserDefinedWritability(1, false);
            Message online = presence(type);
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            when(redis.hasKey(LOGIN_USERINFO_ACCESSTOKEN_KEY + "access-" + LEGACY_DEVICE_ID)).thenReturn(true);
            MQMessageListener listener = new MQMessageListener();
            ReflectionTestUtils.setField(listener, "stringRedisTemplate", redis);
            listener.handleClusterMessage(new ClusterMessageWrapper<>(online, 100L));
            assertSame(online, channel.readOutbound());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void nodeDeliversToAllListedDevicesAndSkipsRevokedConnection() {
        EmbeddedChannel desktop = new EmbeddedChannel(DefaultChannelId.newInstance());
        EmbeddedChannel app = new EmbeddedChannel(DefaultChannelId.newInstance());
        try {
            addChannel(desktop, "desktop");
            addChannel(app, "app");
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
            when(redis.opsForHash()).thenReturn(hashes);
            when(hashes.get(deviceTokenKey(user.getId(), "desktop"), "connectionId"))
                    .thenReturn(desktop.id().asLongText());
            when(hashes.get(deviceTokenKey(user.getId(), "app"), "connectionId"))
                    .thenReturn(app.id().asLongText());
            MQMessageListener listener = new MQMessageListener();
            ReflectionTestUtils.setField(listener, "stringRedisTemplate", redis);
            Message online = presence(9);
            var wrapper = new ClusterMessageWrapper<>(online, user.getId(),
                    List.of(desktop.id().asLongText(), app.id().asLongText()));
            listener.handleClusterMessage(wrapper);
            assertSame(online, desktop.readOutbound());
            assertSame(online, app.readOutbound());

            when(hashes.get(deviceTokenKey(user.getId(), "app"), "connectionId")).thenReturn(null);
            listener.handleClusterMessage(wrapper);
            assertSame(online, desktop.readOutbound());
            assertNull(app.readOutbound());
        } finally {
            desktop.finishAndReleaseAll();
            app.finishAndReleaseAll();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void onlyTheCurrentConnectionOfADeviceCanSendMessages() {
        EmbeddedChannel old = new EmbeddedChannel(DefaultChannelId.newInstance());
        EmbeddedChannel current = new EmbeddedChannel(DefaultChannelId.newInstance());
        try {
            addChannel(old, "desktop_one");
            addChannel(current, "desktop_one");
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
            when(redis.opsForHash()).thenReturn(hashes);
            String deviceKey = deviceTokenKey(user.getId(), "desktop_one");
            when(hashes.get(deviceKey, "connectionId")).thenReturn(old.id().asLongText());
            assertTrue(ChannelManageUtil.isCurrent(old, redis));

            // 新登录清除了登记，旧连接不能在新握手前继续发送。
            when(hashes.get(deviceKey, "connectionId")).thenReturn(null);
            assertFalse(ChannelManageUtil.isCurrent(old, redis));

            when(hashes.get(deviceKey, "connectionId")).thenReturn(current.id().asLongText());
            assertFalse(ChannelManageUtil.isCurrent(old, redis));
            assertTrue(ChannelManageUtil.isCurrent(current, redis));
            verify(redis, never()).hasKey(anyString());
        } finally {
            old.finishAndReleaseAll();
            current.finishAndReleaseAll();
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
            addChannel(old);
            addChannel(current);
            old.finishAndReleaseAll();
            assertEquals(List.of(current), ChannelManageUtil.getChannels(100L));
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
    void clusterDeviceUpdatesUseLiveRoutesAndDoNotNotifyOtherAccounts() throws Exception {
        EmbeddedChannel desktop = new EmbeddedChannel(DefaultChannelId.newInstance());
        try {
            addChannel(desktop, "desktop_one");
            StringRedisTemplate redis = mock(StringRedisTemplate.class);
            HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
            ValueOperations<String, String> values = mock(ValueOperations.class);
            when(redis.opsForHash()).thenReturn(hashes);
            when(redis.opsForValue()).thenReturn(values);
            when(hashes.get(deviceTokenKey(user.getId(), "desktop_one"), "connectionId"))
                    .thenReturn(desktop.id().asLongText());
            when(hashes.get(USER_CLUSTER_MAPPING_KEY, "100")).thenReturn(JSON.toJSONString(Map.of(
                    "desktop_one", List.of(NettyConfig.NETTY_CLUSTER_ID, desktop.id().asLongText()),
                    "app_one", List.of("other-node", "first"),
                    "app_two", List.of("other-node", "second"))));
            when(values.multiGet(any())).thenAnswer(invocation ->
                    invocation.<List<String>>getArgument(0).stream()
                            .map(key -> key.endsWith(":first") ? null : "1")
                            .toList());

            MQMessageListener listener = new MQMessageListener();
            ReflectionTestUtils.setField(listener, "stringRedisTemplate", redis);
            listener.handleClusterMessage(new ClusterMessageWrapper<>(new DevicePresenceResponseVO(), 200L));
            desktop.runPendingTasks();
            assertNull(desktop.readOutbound());

            ObjectMapper mapper = new ObjectMapper();
            byte[] encoded = mapper.writeValueAsBytes(new ClusterMessageWrapper<>(new DevicePresenceResponseVO(), 100L));
            var update = mapper.readValue(encoded, new TypeReference<ClusterMessageWrapper<Message>>() {});
            listener.handleClusterMessage(update);
            desktop.runPendingTasks();
            DevicePresenceResponseVO snapshot = desktop.readOutbound();
            assertTrue(snapshot.isDesktopOnline());
            assertTrue(snapshot.isAppOnline());

            doAnswer(invocation ->
                    invocation.<List<String>>getArgument(0).stream()
                            .map(key -> key.endsWith(desktop.id().asLongText()) ? "1" : null)
                            .toList()).when(values).multiGet(any());
            listener.handleClusterMessage(update);
            listener.handleClusterMessage(update);
            desktop.runPendingTasks();
            snapshot = desktop.readOutbound();
            assertTrue(snapshot.isDesktopOnline());
            assertFalse(snapshot.isAppOnline());
            assertNull(desktop.readOutbound());
        } finally {
            desktop.finishAndReleaseAll();
        }
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
            addChannel(channel);
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
    @SuppressWarnings("unchecked")
    void heartbeatRenewsDeviceLivenessWithoutQueryingFriendList() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(redis.opsForValue()).thenReturn(values);
        HeartBeatHandler handler = new HeartBeatHandler();
        ReflectionTestUtils.setField(handler, "stringRedisTemplate", redis);
        EmbeddedChannel channel = new EmbeddedChannel(handler);
        try {
            addChannel(channel);
            when(redis.hasKey(LOGIN_USERINFO_ACCESSTOKEN_KEY + "access-" + LEGACY_DEVICE_ID)).thenReturn(true);
            HeartRequestDTO heartbeat = new HeartRequestDTO();
            heartbeat.setSequenceId(37);
            channel.writeInbound(heartbeat);
            Message pong = channel.readOutbound();
            assertEquals(0, pong.getMessageType());
            assertEquals(37, pong.getSequenceId());
            assertInstanceOf(DevicePresenceResponseVO.class, channel.readOutbound());
            assertNull(channel.readOutbound());
            verify(values).set("im:device:presence:" + channel.id().asLongText(), "1", Duration.ofSeconds(90));
            verify(redis, never()).opsForSet();
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
