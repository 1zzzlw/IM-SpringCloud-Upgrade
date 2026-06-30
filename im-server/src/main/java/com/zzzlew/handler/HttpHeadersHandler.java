package com.zzzlew.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.utils.ChannelManageUtil;
import com.zzzlew.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/18 - 11 - 18 - 22:47
 * @Description: com.zzzlew.zzzimserver.websocket
 * @version: 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class HttpHeadersHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest request) {
            try {
                log.info("检测到 HttpRequest，开始处理");
                // 获得url路径
                String uri = request.uri();
                String token = getToken(uri);
                // TODO token解析失败，之后加上前端的异常状态
                if (StrUtil.isBlank(token) || token.length() < 32) {
                    log.error("请求路径 {} 中未包含 token 参数，或 token 不合法", uri);
                    ctx.channel().close();
                    return;
                }
                log.info("请求路径 {} 中 token 参数值为 {}", uri, token);
                // 解析token
                Environment env = SpringUtil.getBean(Environment.class);
                String accessTokenSecret = env.getProperty("jwt.accessToken.secret");
                Claims claims = JwtUtil.parseJWT(accessTokenSecret, token);
                // 将用户信息转为用户基本信息DTO UserBaseDTO
                UserBaseDTO userBaseDTO = BeanUtil.copyProperties(claims, UserBaseDTO.class);
                // 打印用户基本信息DTO
                log.info("用户基本信息DTO：{}", userBaseDTO);
                ChannelManageUtil.addChannel(userBaseDTO, ctx.channel());
                log.info("HttpRequest 处理完成，准备升级为 WebSocket 协议");
                ctx.fireChannelRead(msg);
            } catch (Exception e) {
                log.error("HttpRequest 处理异常", e);
                request.release();
                ctx.channel().close();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    public String getToken(String url) {
        if (StrUtil.isBlank(url) || !url.contains("?")) {
            return null;
        }
        String[] queryParams = url.split("\\?");
        if (queryParams.length != 2) {
            return null;
        }
        String[] params = queryParams[1].split("=");
        if (params.length != 2) {
            return null;
        }
        return params[1];
    }
}
