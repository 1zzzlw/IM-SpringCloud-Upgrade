package com.zzzlew.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 23:02
 * @Description: com.zzzlew.zzzimserver.result
 * @version: 1.0
 */
@Data
public class PageResult<T> implements Serializable {

    // 总记录数
    private Long total;
    // 数据列表
    private List<T> data;

}
