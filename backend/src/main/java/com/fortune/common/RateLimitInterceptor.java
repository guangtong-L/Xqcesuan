package com.fortune.common;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单机内存令牌桶（按分钟切片）。
 * <p>
 * key = openid + ":" + path + ":" + 当前分钟
 * <p>
 * <strong>线上风险</strong>：
 * 1) 单机内存，多实例下用户可被分散到不同节点绕过限流；
 * 2) 切 Redis + Lua 后才能保证全局阈值，TODO 见 README。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final OpenIdResolver openIdResolver;

    @Value("${fortune.rate-limit.calc-per-min:10}")
    private int calcPerMin;

    @Value("${fortune.rate-limit.unlock-per-min:5}")
    private int unlockPerMin;

    private final ConcurrentHashMap<String, AtomicInteger> bucket = new ConcurrentHashMap<>();

    /** 简单的过期清理：每 256 次请求触发一次 */
    private final AtomicInteger gcCounter = new AtomicInteger(0);

    public RateLimitInterceptor(OpenIdResolver openIdResolver) {
        this.openIdResolver = openIdResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        int limit = chooseLimit(path);
        if (limit <= 0) {
            return true;
        }
        String openid = openIdResolver.resolve(request);
        if (StrUtil.isBlank(openid)) {
            openid = "anonymous";
        }
        long minute = System.currentTimeMillis() / 60_000L;
        String key = openid + ":" + path + ":" + minute;

        AtomicInteger counter = bucket.computeIfAbsent(key, k -> new AtomicInteger(0));
        int current = counter.incrementAndGet();
        gcIfNeeded(minute);

        if (current > limit) {
            log.warn("限流触发 openid={} path={} current={} limit={}", openid, path, current, limit);
            throw new BizException(ErrorCode.RATE_LIMITED);
        }
        return true;
    }

    private int chooseLimit(String path) {
        if (path == null) {
            return -1;
        }
        if (path.startsWith("/api/fortune/calc")) {
            return calcPerMin;
        }
        if (path.startsWith("/api/fortune/unlock")) {
            return unlockPerMin;
        }
        if (path.startsWith("/api/auth/login")) {
            // 登录给一个相对宽松的阈值，避免合法重试被拦
            return 30;
        }
        return -1;
    }

    /**
     * 清理 2 分钟前的桶，避免内存无限增长
     */
    private void gcIfNeeded(long currentMinute) {
        if ((gcCounter.incrementAndGet() & 0xFF) != 0) {
            return;
        }
        bucket.entrySet().removeIf(e -> {
            String[] parts = e.getKey().split(":");
            try {
                long m = Long.parseLong(parts[parts.length - 1]);
                return currentMinute - m > 2;
            } catch (NumberFormatException ex) {
                return true;
            }
        });
    }
}
