package com.zzzlew.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * @Auther: zzzlew
 * @Date: 2025/12/13 - 12 - 13 - 18:04
 * @Description: com.zzzlew.zzzimserver.properties
 * @version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinIOConfigProperties implements Serializable {

    /**
     * MinIO 服务端地址
     */
    private String endpoint;

    /**
     * MinIO 访问密钥
     */
    private String accessKey;

    /**
     * MinIO 密钥
     */
    private String secretKey;

    /**
     * 存储用户头像桶的名称
     */
    private String avatarBucket;

    /**
     * 存储照片桶的名称
     */
    private String imageBucket;

    /**
     * 存储视频桶的名称
     */
    private String videoBucket;

    /**
     * 存储音频桶的名称
     */
    private String AudioBucket;

    /**
     * 存储文件桶的名称
     */
    private String fileBucket;

    /**
     * 存储笔记文件桶的名称
     */
    private String favoriteBucket;

    /**
     * MinIO 图片路径
     */
    private String imagePath;

    /**
     * MinIO 文件路径
     */
    private String filePath;

    /**
     * MinIO 视频路径
     */
    private String videoPath;

    /**
     * MinIO 过期时间
     */
    private Integer expireIn;

}
