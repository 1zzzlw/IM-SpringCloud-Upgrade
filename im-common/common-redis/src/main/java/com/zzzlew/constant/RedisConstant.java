package com.zzzlew.constant;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/9 - 11 - 09 - 22:00
 * @Description: com.zzzlew.zzzimserver.constant
 * @version: 1.0
 */
public class RedisConstant {

    public static final String LOGIN_USER_TOKEN_LIST_KEY = "login:user:tokenList:";
    public static final Long LOGIN_USER_TOKEN_LIST_KEY_TTL = 36000L;

    public static final String LOGIN_CODE_KEY = "login:code:";
    public static final Long LOGIN_CODE_TTL = 2L;

    public static final String LOGIN_USERINFO_ACCESSTOKEN_KEY = "login:user:Info:accessToken:";
    public static final Long LOGIN_USERINFO_ACCESSTOKEN_KEY_TTL = 36000L;

    public static final String LOGIN_USERINFO_REFRESHTOKEN_KEY = "login:user:Info:refreshToken:";
    public static final Long LOGIN_USERINFO_REFRESHTOKEN_KEY_TTL = 36000L;

    public static final String REGISTER_CODE_KEY = "register:code:";
    public static final Long REGISTER_CODE_TTL = 2L;

    public static final String USER_FRIEND_LIST_KEY = "user:friend:list:";
    public static final Long USER_FRIEND_LIST_KEY_TTL = 2L;

    public static final String USER_ONLINE_STATUS_KEY = "user:online:status";
    // 270秒过期时间，因为用户在270秒内没有操作，就可以判断为用户不在线，对应心跳时间90秒
    public static final Long USER_ONLINE_STATUS_KEY_TTL = 270L;

    // 上传文件的凭证目录
    public static final String FILE_UPLOAD_VERIFY_KEY = "file:upload:verify:";
    public static final Long FILE_UPLOAD_VERIFY_KEY_TTL = 5L;

    // 分块文件目录的键
    public static final String FILE_CHUNK_INDEX_KEY = "file:chunk:index:";
    public static final Long FILE_CHUNK_INDEX_KEY_TTL = 5L;

    public static final String USER_OFFLINE_INFO_KEY = "user:offline:quitTime";

    public static final String SYSTEM_MESSAGE_BROADCAST = "system:message:broadcast";

    public static final String USER_CLUSTER_MAPPING_KEY = "netty:websocket:cluster:info";

    // 用户离线消息存储的集合
    public static final String USER_OFFLINE_MESSAGE_CONTENT_KEY = "user:offline:message:content:";
    public static final Long USER_OFFLINE_MESSAGE_KEY_TTL = 20L;
}
