package com.zzzlew.server;

import com.zzzlew.domain.dto.AIMessageDTO;
import com.zzzlew.domain.dto.AIPersonalityDTO;
import com.zzzlew.domain.vo.AIMessageVO;
import com.zzzlew.domain.vo.AIPersonalityVO;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/5 - 03 - 05 - 13:51
 * @Description: com.zzzlew.server
 * @version: 1.0
 */
public interface AIMessageService {

    /**
     * 加载历史消息
     *
     * @return
     */
    List<AIMessageVO> loadMessage();

    /**
     * 发送消息
     *
     * @param aiMessageDTO
     * @return
     */
    Flux<String> sendMessage(AIMessageDTO aiMessageDTO);

    /**
     * 更新ai个性化配置
     *
     * @param aiPersonalityDTO
     */
    void updatePersonality(AIPersonalityDTO aiPersonalityDTO);

    /**
     * 创建ai个性化配置
     *
     * @param aiPersonalityDTO
     * @param avatarFile
     */
    String createPersonality(AIPersonalityDTO aiPersonalityDTO, MultipartFile avatarFile);

    /**
     * 切换ai个性化配置
     *
     * @param id
     */
    String switchPersonality(Long id);

    /**
     * 获取ai个性化配置列表
     *
     * @return
     */
    List<AIPersonalityVO> listPersonality();

    /**
     * 删除ai个性化配置
     *
     * @param id
     */
    void deletePersonality(Long id);
}
