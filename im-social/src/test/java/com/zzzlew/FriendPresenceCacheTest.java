package com.zzzlew;

import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.vo.FriendRelationVO;
import com.zzzlew.mapper.FriendMapper;
import com.zzzlew.server.impl.FriendServiceImpl;
import com.zzzlew.utils.UserHolder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static com.zzzlew.constant.RedisConstant.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FriendPresenceCacheTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @SuppressWarnings("unchecked")
    void fullFriendSyncRebuildsPresenceRecipientsIncludingEmptyList(boolean empty) {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        HashOperations<String, Object, Object> hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        when(hashes.entries(USER_OFFLINE_INFO_KEY)).thenReturn(Map.of());
        FriendMapper mapper = mock(FriendMapper.class);
        FriendRelationVO friend = new FriendRelationVO();
        friend.setFriendId(200L);
        when(mapper.selectFriendList(100L, null)).thenReturn(empty ? List.of() : List.of(friend));
        FriendServiceImpl service = new FriendServiceImpl();
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "friendMapper", mapper);
        UserBaseDTO user = new UserBaseDTO();
        user.setId(100L);
        UserHolder.save(user);
        try {
            service.initFriendList(true);
            if (empty) {
                verify(redis).execute(any(DefaultRedisScript.class),
                        eq(List.of(USER_FRIEND_LIST_KEY + "100")), eq("120"));
            } else {
                verify(redis).execute(any(DefaultRedisScript.class),
                        eq(List.of(USER_FRIEND_LIST_KEY + "100")), eq("120"), eq("200"));
            }
        } finally {
            UserHolder.removeUser();
        }
    }
}
