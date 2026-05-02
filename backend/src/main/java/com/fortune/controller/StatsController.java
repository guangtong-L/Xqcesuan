package com.fortune.controller;

import com.fortune.common.ApiResponse;
import com.fortune.dto.resp.StatsResp;
import com.fortune.service.FortuneService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 后台统计接口，本期返回内存计数器
 * 线上风险：建议加 IP 白名单或 Basic Auth，否则被刷会污染统计
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final FortuneService fortuneService;

    @GetMapping("/today")
    public ApiResponse<StatsResp> today() {
        long totalCalc = fortuneService.getTotalCalc();
        long uniqueUser = fortuneService.getUniqueUserToday();
        long unlockCount = fortuneService.getUnlockCount();
        // 假估算：激励视频 0.05 元 + 普通广告 0.005 元 / 次
        BigDecimal revenue = BigDecimal.valueOf(unlockCount * 0.05 + totalCalc * 0.005)
                .setScale(2, RoundingMode.HALF_UP);
        StatsResp resp = StatsResp.builder()
                .date(fortuneService.getStatsDate())
                .totalCalc(totalCalc)
                .uniqueUser(uniqueUser)
                .unlockCount(unlockCount)
                .adRevenueEstimate(revenue.doubleValue())
                .build();
        return ApiResponse.success(resp);
    }
}
