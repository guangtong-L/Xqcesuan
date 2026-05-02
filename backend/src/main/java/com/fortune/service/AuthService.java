package com.fortune.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.fortune.dto.req.LoginReq;
import com.fortune.dto.resp.LoginResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * 鉴权服务
 * <p>
 * <strong>线上风险</strong>：本期 mock，未走 wx code2Session。
 * 上线必须替换：调用微信接口换 openid + session_key，并自管 token（建议 JWT）。
 */
@Slf4j
@Service
public class AuthService {

    @Value("${fortune.token.expire-days:7}")
    private int expireDays;

    public LoginResp login(LoginReq req) {
        String code = req.getCode();
        // 假 openid：基于 code 哈希前 8 位
        String openid = "mock_openid_" + DigestUtil.md5Hex(code).substring(0, 8);
        long now = System.currentTimeMillis();
        String token = openid + "_" + now;

        OffsetDateTime expireAt = OffsetDateTime.now(ZoneOffset.of("+08:00"))
                .plusDays(expireDays);

        log.info("[mock 登录] code={} openid={} expireAt={}", code, openid, expireAt);
        return LoginResp.builder()
                .openid(openid)
                .token(token)
                .expireAt(expireAt)
                .isNew(true)
                .build();
    }
}
