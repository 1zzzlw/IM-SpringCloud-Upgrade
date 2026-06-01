package com.zzzlew.mapper;


import com.zzzlew.domain.dto.AIMessageDTO;
import com.zzzlew.domain.dto.AIPersonalityDTO;
import com.zzzlew.domain.vo.AIMessageVO;
import com.zzzlew.domain.vo.AIPersonalityVO;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/5 - 03 - 05 - 14:02
 * @Description: com.zzzlew.mapper
 * @version: 1.0
 */
public interface AIMessageMapper {

    /**
     * 获取历史消息
     *
     * @param userId
     * @return
     */
    List<AIMessageVO> selectHistory(Long userId);

    /**
     * 保存消息
     *
     * @param aiMessageDTO
     */
    void saveMessage(AIMessageDTO aiMessageDTO);

    /**
     * 更新用户个性化
     *
     * @param id
     * @param roleName
     * @param content
     */
    void updatePersonality(Long id, String roleName, String content);

    /**
     * 取消所有ai个性化
     *
     * @param userId
     */
    void cancelAllAIPersonality(Long userId);

    /**
     * 切换用户个性化
     *
     * @param id
     * @param userId
     */
    void switchPersonality(Long id, Long userId);

    /**
     * 创建用户个性化
     *
     * @param aiPersonalityDTO
     */
    void createPersonality(AIPersonalityDTO aiPersonalityDTO);

    /**
     * 获取用户个性化列表
     *
     * @return
     */
    List<AIPersonalityVO> getPersonality(Long userId);

    /**
     * 获取ai头像
     *
     * @param id
     * @param userId
     * @return
     */
    String getAiAvatar(Long id, Long userId);


    /**
     * 删除用户个性化
     *
     * @param id
     * @param userId
     */
    void deletePersonality(Long id, Long userId);

    /**
     * 获取当前用户激活的ai个性化
     *
     * @param userId
     * @return
     */
    AIPersonalityVO getActivePersonality(Long userId);

    /**
     * 获取ai头像，根据激活情况
     *
     * @param userId
     * @return
     */
    String getAiAvatarById(Long userId);
}
