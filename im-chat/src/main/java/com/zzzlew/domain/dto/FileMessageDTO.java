package com.zzzlew.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Auther: zzzlew
 * @Date: 2025/12/10 - 12 - 10 - 19:14
 * @Description: com.zzzlew.zzzimserver.pojo.dto.message
 * @version: 1.0
 */
@Data
public class FileMessageDTO implements Serializable {

    /**
     * 文件哈希值
     */
    private String fileHash;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型
     */
    private Integer fileType;

    /**
     * minio的文件路径
     */
    private String minioFilePath;

    /**
     * 分块数量
     */
    private Integer chunkCount;
}
