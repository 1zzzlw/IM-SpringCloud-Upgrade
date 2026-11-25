package com.zzzlew.domain;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/24 - 03 - 24 - 16:17
 * @Description: com.zzzlew.domain
 * @version: 1.0
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 集群消息包装类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClusterMessageWrapper<T> {
    private T message;
    private Long targetUserId;
    private List<String> targetConnectionIds;

    public ClusterMessageWrapper(T message, Long targetUserId) {
        this(message, targetUserId, null);
    }

    public ClusterMessageWrapper(T message) {
        this.message = message;
    }
}
