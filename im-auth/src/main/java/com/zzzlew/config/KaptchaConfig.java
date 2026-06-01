package com.zzzlew.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/9 - 11 - 09 - 17:32
 * @Description: com.zzzlew.zzzimserver.config
 * @version: 1.0
 */
@Configuration
public class KaptchaConfig {

    @Bean
    public static DefaultKaptcha kaptchaProducer() {
        // 配置验证码属性
        Properties properties = new Properties();
        // 验证码图片宽度
        properties.setProperty("kaptcha.image.width", "120");
        // 验证码图片高度
        properties.setProperty("kaptcha.image.height", "40");
        // 验证码字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        // 验证码字体颜色（RGB）
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");
        // 验证码字体（可多字体，用逗号分隔）
        properties.setProperty("kaptcha.textproducer.font.names", "宋体,楷体,微软雅黑");
        // 验证码字符长度
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 验证码字符集（排除易混淆字符：0,1,o,i,l）
        properties.setProperty("kaptcha.textproducer.char.string",
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjklmnpqrstuvwxyz");
        // 噪声实现类（用DefaultNoise，这是真正的噪声生成器）
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.DefaultNoise");
        // 干扰线颜色（灰色）
        properties.setProperty("kaptcha.noise.color", "128,128,128");
        // 图片扭曲效果（配置WaterRipple，这才是它该在的位置）
        properties.setProperty("kaptcha.distort.impl", "com.google.code.kaptcha.impl.WaterRipple");
        // 背景渐变颜色（上：浅灰，下：白）
        properties.setProperty("kaptcha.background.clear.from", "245,245,245");
        properties.setProperty("kaptcha.background.clear.to", "255,255,255");

        // 初始化验证码生成器
        Config config = new Config(properties);
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        kaptcha.setConfig(config);
        return kaptcha;
    }

}
