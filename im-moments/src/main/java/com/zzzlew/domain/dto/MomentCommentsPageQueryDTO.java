package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/8 - 06 - 08 - 23:32
 * @Description: com.zzzlew.domain.dto
 * @version: 1.0
 */
@Data
public class MomentCommentsPageQueryDTO {
    // 帖子id
    private Long momentId;

    // 页码
    private int page;

    // 每页显示记录数
    private int pageSize;
}
