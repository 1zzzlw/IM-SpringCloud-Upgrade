package com.zzzlew.domain.vo;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/15 - 11 - 15 - 0:08
 * @Description: com.zzzlew.zzzimserver.pojo.vo.user
 * @version: 1.0
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApplyVO implements Serializable {

    /**
     * 申请表id
     */
    private Long applyId;

    /**
     * 申请好友id
     */
    private Long fromUserId;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 用户名
     */
    private String username;

    /**
     * 账号
     */
    private String account;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 生日
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime birthday;

    /**
     * 地址
     */
    private String address;

    /**
     * 申请信息
     */
    private String applyMsg;

    /**
     * 0：未处理，1：已处理
     */
    private Integer isDealt;

    /**
     * 0：拒绝，1：同意
     */
    private Integer dealResult;

}
