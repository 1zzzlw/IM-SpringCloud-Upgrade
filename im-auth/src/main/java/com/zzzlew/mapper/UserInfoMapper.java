package com.zzzlew.mapper;

import com.zzzlew.domain.entity.UserInfo;
import com.zzzlew.domain.vo.UserSearchVO;
import org.apache.ibatis.annotations.Select;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/10 - 11 - 10 - 22:34
 * @Description: com.zzzlew.zzzimserver.mapper
 * @version: 1.0
 */
public interface UserInfoMapper {
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
     * @param phone 手机号或账号
     * @return 用户信息
     */
    UserSearchVO getByPhoneOrAccount(Long userId, String phone);

}
