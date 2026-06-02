package com.zzzlew.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/6 - 03 - 06 - 19:58
 * @Description: com.zzzlew.tools
 * @version: 1.0
 */
@Component
public class AITools {

    @Tool(description = "获取系统当前的日期、时间和星期")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        String dateTime = now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
        String weekday = now.format(DateTimeFormatter.ofPattern("EEEE", Locale.CHINA));
        return dateTime + " " + weekday;
    }
}
