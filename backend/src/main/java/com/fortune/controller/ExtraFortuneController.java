package com.fortune.controller;

import com.fortune.common.ApiResponse;
import com.fortune.common.OpenIdResolver;
import com.fortune.dto.resp.SignResp;
import com.fortune.service.SignService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 每日一签活动接口。
 * 同一用户当日抽签结果一致，跨日刷新。
 */
@RestController
@RequestMapping("/api/fortune")
@RequiredArgsConstructor
public class ExtraFortuneController {

    private final SignService signService;
    private final OpenIdResolver openIdResolver;

    @GetMapping("/sign")
    public ApiResponse<SignResp> sign() {
        String openid = openIdResolver.currentOpenId();
        return ApiResponse.success(signService.draw(openid));
    }
}
