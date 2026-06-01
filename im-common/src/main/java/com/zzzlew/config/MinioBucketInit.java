package com.zzzlew.config;

import com.zzzlew.properties.MinIOConfigProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.MinioException;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * @Auther: zzzlew
 * @Date: 2026/1/29 - 01 - 29 - 18:10
 * @Description: com.zzzlew.zzzimserver.config
 * @version: 1.0
 */
@Slf4j
@Component
public class MinioBucketInit {
    @Resource
    private MinIOConfigProperties minIOConfigProperties;
    @Resource
    private MinioClient minioClient;

    @PostConstruct
    public void createBucketsOnStart() {
        // 待创建的桶名列表
        String[] bucketNames = {minIOConfigProperties.getImageBucket(), minIOConfigProperties.getVideoBucket(),
                minIOConfigProperties.getFileBucket(), minIOConfigProperties.getAudioBucket(),
                minIOConfigProperties.getAvatarBucket(), minIOConfigProperties.getFavoriteBucket()};

        for (String bucketName : bucketNames) {
            try {
                // 检测桶是否存在
                if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                    // 桶不存在则创建
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                    log.info("MinIO桶创建成功：{}", bucketName);
                } else {
                    log.info("MinIO桶已存在，无需重复创建：{}", bucketName);
                }

                String publicReadPolicy = buildPublicReadPolicy(bucketName);
                minioClient.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucketName).config(publicReadPolicy) // 传入公共读策略JSON
                        .build());
                log.info("MinIO桶[{}]公共读权限设置成功", bucketName);
            } catch (MinioException | InvalidKeyException | NoSuchAlgorithmException | IOException e) {
                log.error("MinIO桶创建失败：{}，原因：{}", bucketName, e.getMessage(), e);
                throw new RuntimeException("MinIO初始化失败，桶创建异常：" + bucketName, e);
            }
        }
    }

    /**
     * MinIO公共读权限标准策略 仅开放「所有用户」对「桶内所有文件」的GET操作（读），写/删权限仍为私有
     *
     * @param bucketName 桶名
     * @return 公共读策略JSON字符串
     */
    private String buildPublicReadPolicy(String bucketName) {
        if (Objects.isNull(bucketName) || bucketName.trim().isEmpty()) {
            throw new IllegalArgumentException("MinIO桶名不能为空");
        }
        // 固定策略模板，仅替换桶名占位符，无需修改其他内容
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": "s3:GetObject",
                      "Resource": "arn:aws:s3:::%s/*"
                    }
                  ]
                }
                """.formatted(bucketName);
    }
}
