package com.fortune.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 统计响应
 */
@Data
@Builder
public class StatsResp {
    private String date;
    private Long totalCalc;
    private Long uniqueUser;
    private Long unlockCount;
    private Double adRevenueEstimate;
}
