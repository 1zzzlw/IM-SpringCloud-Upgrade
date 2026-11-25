package com.zzzlew.handler;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.zzzlew.constant.JwtClaimsConstant;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.utils.ChannelManageUtil;
import com.zzzlew.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static com.zzzlew.constant.RedisConstant.*;

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
                String token = getToken(request.uri());
                Environment env = SpringUtil.getBean(Environment.class);
                Claims claims = token == null ? null : JwtUtil.parseJWT(env.getProperty("jwt.accessToken.secret"), token);
                if (claims == null || claims.get(JwtClaimsConstant.USER_ID) == null) {
                    request.release();
                    ctx.close();
                    return;
                }
                UserBaseDTO user = BeanUtil.copyProperties(claims, UserBaseDTO.class);
                String deviceId = claims.get(DEVICE_ID_CLAIM, String.class);
                String accessKey = LOGIN_USERINFO_ACCESSTOKEN_KEY + token;
                if (!Boolean.TRUE.equals(SpringUtil.getBean(StringRedisTemplate.class).hasKey(accessKey))) {
                    request.release();
                    ctx.close();
                    return;
                }
                ChannelManageUtil.authenticate(ctx.channel(), user, deviceId, token);
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
        List<String> tokens = new QueryStringDecoder(url).parameters().get("token");
        return tokens != null && tokens.size() == 1 && !tokens.get(0).isBlank() ? tokens.get(0) : null;
    }
}
