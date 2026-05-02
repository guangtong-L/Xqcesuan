package com.fortune.controller;

import com.fortune.common.ApiResponse;
import com.fortune.common.OpenIdResolver;
import com.fortune.dto.req.BaziReq;
import com.fortune.dto.req.MatchReq;
import com.fortune.dto.resp.BaziResp;
import com.fortune.dto.resp.MatchResp;
import com.fortune.dto.resp.SignResp;
import com.fortune.service.BaziService;
import com.fortune.service.MatchService;
import com.fortune.service.SignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 三个扩展功能：八字流年 / 星座匹配 / 幸运签
 * 全部 mock，同一用户/输入当日结果一致
 */
@Slf4j
@RestController
@RequestMapping("/api/fortune")
@RequiredArgsConstructor
public class ExtraFortuneController {

    private final BaziService baziService;
    private final MatchService matchService;
    private final SignService signService;
    private final OpenIdResolver openIdResolver;

    @PostMapping("/bazi")
    public ApiResponse<BaziResp> bazi(@RequestBody @Valid BaziReq req) {
        String openid = openIdResolver.currentOpenId();
        log.info("[bazi] openid={} birthday={}", openid, req.getBirthday());
        return ApiResponse.success(baziService.calc(req, openid));
    }

    @PostMapping("/match")
    public ApiResponse<MatchResp> match(@RequestBody @Valid MatchReq req) {
        log.info("[match] {} x {}", req.getZodiacA(), req.getZodiacB());
        return ApiResponse.success(matchService.match(req));
    }

    @GetMapping("/sign")
    public ApiResponse<SignResp> sign() {
        String openid = openIdResolver.currentOpenId();
        return ApiResponse.success(signService.draw(openid));
    }
}
