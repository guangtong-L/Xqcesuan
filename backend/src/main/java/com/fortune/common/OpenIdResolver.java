package com.fortune.common;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * openid 解析器
 * <p>
 * 优先级：
 * 1. 微信云托管自动注入的请求头 X-WX-OPENID
 * 2. 自定义 Authorization 头（约定格式 mock_openid_xxx_timestamp）
 * 3. 兜底返回 mock_openid_test，方便本地联调
 *
 * <strong>线上风险</strong>：兜底必须移除，否则任何匿名请求都会落到同一桶导致越权读写。
 */
@Component
public class OpenIdResolver {

    public static final String HEADER_OPENID = "X-WX-OPENID";
    public static final String HEADER_AUTH = "Authorization";
    public static final String FALLBACK_OPENID = "mock_openid_test";

    public String currentOpenId() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return FALLBACK_OPENID;
        }
        return resolve(attrs.getRequest());
    }

    public String resolve(HttpServletRequest request) {
        String openid = request.getHeader(HEADER_OPENID);
        if (StrUtil.isNotBlank(openid)) {
            return openid;
        }
        String auth = request.getHeader(HEADER_AUTH);
        if (StrUtil.isNotBlank(auth)) {
            // token 约定：mock_openid_xxxx_timestamp
            String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;
            int idx = token.lastIndexOf("_");
            if (idx > 0 && token.startsWith("mock_openid_")) {
                return token.substring(0, idx);
            }
        }
        return FALLBACK_OPENID;
    }
}
