package com.zzzlew.utils;

import com.zzzlew.properties.MinIOConfigProperties;
import io.minio.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Auther: zzzlew
 * @Date: 2025/12/13 - 12 - 13 - 18:10
 * @Description: com.zzzlew.zzzimserver.utils
 * @version: 1.0
 */
@Slf4j
@Component
public class MinIOFileStorgeUtil {

    @Resource
    private MinioClient minioClient;

    @Resource
    private MinIOConfigProperties minIOConfigProperties;

    private final static String separator = "/";

    /**
     * 构建文件路径
     *
     * @param filename 文件名
     * @return 文件路径 格式：yyyy/MM/dd/filename
     */
    public String buildFilePath(String filename) {
        StringBuilder stringBuilder = new StringBuilder(50);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String todayStr = sdf.format(new Date());
        stringBuilder.append(todayStr).append(separator);
        stringBuilder.append(filename);
        return stringBuilder.toString();
    }

    // 上传用户头像到minio
    public void uploadAvatar(String minioUserAvatarPath, MultipartFile avatarBlob) {
        try {
            // 根据分块文件的流存入minio
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    // 存储桶名称
                    .bucket(minIOConfigProperties.getAvatarBucket())
                    // 存入minio的路径对象
                    .object(minioUserAvatarPath)
                    // 输入流
                    .stream(avatarBlob.getInputStream(), avatarBlob.getSize(), -1)
                    // 内容类型
                    .contentType(avatarBlob.getContentType()).build();
            // 上传文件分块到minio
            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{}错误信息:{}", minIOConfigProperties.getAvatarBucket(), e.getMessage());
        }
    }

    // 上传笔记中的图片
    public void uploadFavoriteImage(String minioImagePath, MultipartFile imageBlob) {
        try {
            // 根据分块文件的流存入minio
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    // 存储桶名称
                    .bucket(minIOConfigProperties.getFavoriteBucket())
                    // 存入minio的路径对象
                    .object(minioImagePath)
                    // 输入流
                    .stream(imageBlob.getInputStream(), imageBlob.getSize(), -1)
                    // 内容类型
                    .contentType(imageBlob.getContentType()).build();
            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{}错误信息:{}", minIOConfigProperties.getFavoriteBucket(), e.getMessage());
        }
    }

    // 上传朋友圈中的图片
    public void uploadMomentsImage(String minioImagePath, MultipartFile imageBlob) {
        try {
            // 根据分块文件的流存入minio
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    // 存储桶名称
                    .bucket(minIOConfigProperties.getMomentsBucket())
                    // 存入minio的路径对象
                    .object(minioImagePath)
                    // 输入流
                    .stream(imageBlob.getInputStream(), imageBlob.getSize(), -1)
                    // 内容类型
                    .contentType(imageBlob.getContentType()).build();
            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{}错误信息:{}", minIOConfigProperties.getMomentsBucket(), e.getMessage());
        }
    }

    // 上传文件分块到minio
    public void uploadFileChunk(String minioFileChunkPath, MultipartFile chunkBlob, Integer fileType) {
        try {
            // 根据分块文件的流存入minio
            PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                    // 存储桶名称
                    .bucket(getBucketName(fileType))
                    // 存入minio的路径对象
                    .object(minioFileChunkPath)
                    // 输入流
                    .stream(chunkBlob.getInputStream(), chunkBlob.getSize(), -1)
                    // 内容类型 TODO 后续根据文件类型动态设置，文件分块默认没有类型
                    .contentType(chunkBlob.getContentType()).build();
            // 上传文件分块到minio
            minioClient.putObject(putObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上传文件出错,bucket:{},objectName:{},错误信息:{}", getBucketName(fileType), minioFileChunkPath,
                    e.getMessage());
        }
    }

    /**
     * 合并文件分块
     *
     * @param minioFilePath minio文件路径 格式：yyyy/MM/dd/filename
     * @param chunkCount    分块数量
     */
    public void mergeFileChunks(String minioFilePath, String minioFileChunkPath, int chunkCount, Integer fileType) {
        // 从minio中获得所有的分块文件
        List<ComposeSource> sources = new ArrayList<>();
        for (int i = 0; i < chunkCount; i++) {
            sources.add(ComposeSource.builder().bucket(getBucketName(fileType)).object(minioFileChunkPath + i).build());
        }

        ComposeObjectArgs composeObjectArgs =
                ComposeObjectArgs.builder().bucket(getBucketName(fileType)).object(minioFilePath).sources(sources).build();

        try {
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("合并文件出错,bucket:{},objectName:{},错误信息:{}", getBucketName(fileType), minioFilePath, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 清除文件分块
     *
     * @param minioFileChunkPath minio文件分块路径 格式：yyyy/MM/dd/filename/
     * @param chunkCount         分块数量
     */
    public void clearChunkFlies(String minioFileChunkPath, int chunkCount, Integer fileType) {
        Iterable<DeleteObject> objects = Stream.iterate(0, i -> ++i).limit(chunkCount)
                .map(i -> new DeleteObject(minioFileChunkPath + i)).collect(Collectors.toList());

        RemoveObjectsArgs removeObjectsArgs =
                RemoveObjectsArgs.builder().bucket(getBucketName(fileType)).objects(objects).build();

        try {
            // 防止命名冲突
            Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
            results.forEach(f -> {
                try {
                    DeleteError deleteError = f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            log.error("清除文件分块出错,bucket:{},objectName:{},错误信息:{}", getBucketName(fileType), minioFileChunkPath,
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String getBucketName(Integer fileType) {
        switch (fileType) {
            case 2 -> {
                return minIOConfigProperties.getImageBucket();
            }
            case 3 -> {
                return minIOConfigProperties.getVideoBucket();
            }
            case 4 -> {
                return minIOConfigProperties.getAudioBucket();
            }
            case 5 -> {
                return minIOConfigProperties.getFileBucket();
            }
            default -> throw new IllegalArgumentException("不支持的文件类型编码：" + fileType);
        }
    }

}
