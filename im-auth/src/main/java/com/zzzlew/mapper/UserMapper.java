package com.zzzlew.mapper;

import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.entity.UserInfo;
import com.zzzlew.domain.vo.UserSearchVO;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:09
 * @Description: com.zzzlew.zzzimserver.mapper
 * @version: 1.0
 */
public interface UserMapper {
    /**
     * 根据账号查询用户
     *
     * @param account 账号
     * @return 用户实体类
     */
    @Select("select * from user_info where account = #{account}")
    UserInfo getByAccount(String account);

    /**
     * 插入用户
     *
     * @param
     */
    void insertUserAuth(UserAuth userAuth);

    /**
     * 根据用户ID列表查询用户
     *
     * @param targetUserIdList 用户ID列表
     * @return 用户实体类列表
     */
    List<UserAuth> selectUserAuthListByUserIdList(List<Long> targetUserIdList);


    /**
     * 根据用户ID查询用户信息
     *
     * @param userId 用户ID
     * @return 用户信息VO
     */
    UserAuth selectUserInfoById(Long userId);

    /**
     * 插入用户信息
     *
     * @param userInfo 用户信息
     */
    void insertUserInfo(UserInfo userInfo);

    /**
     * 根据手机号查询用户信息
     *
     * @param phone 手机号
     * @return 用户信息
     */
    @Select("select exists(select 1 from user_info where phone = #{phone})")
    boolean getByPhone(String phone);

    /**
     * 根据手机号或账号查询用户信息
     *
     * @param number 手机号或账号
     * @return 用户信息
     */
    UserSearchVO getByPhoneOrAccount(Long userId, String number);
}
