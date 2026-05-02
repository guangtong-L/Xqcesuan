package com.fortune.controller;

import com.fortune.common.ApiResponse;
import com.fortune.dto.req.LoginReq;
import com.fortune.dto.resp.LoginResp;
import com.fortune.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * 微信小程序登录接口（本期 mock，不调微信 code2Session）
     * 线上风险：上线时切到真实 code2Session 调用，并对 token 做 JWT 签名
     */
    @PostMapping("/login")
    public ApiResponse<LoginResp> login(@RequestBody @Valid LoginReq req) {
        log.info("[auth.login] code长度={}", req.getCode() == null ? 0 : req.getCode().length());
        return ApiResponse.success(authService.login(req));
    }
}
