package com.fortune.controller;

import com.fortune.common.ApiResponse;
import com.fortune.common.OpenIdResolver;
import com.fortune.dto.req.FortuneCalcReq;
import com.fortune.dto.req.UnlockReq;
import com.fortune.dto.resp.FortuneResp;
import com.fortune.dto.resp.HistoryListResp;
import com.fortune.dto.resp.UnlockResp;
import com.fortune.service.FortuneService;
import com.fortune.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/fortune")
@RequiredArgsConstructor
@Validated
public class FortuneController {

    private final FortuneService fortuneService;
    private final HistoryService historyService;
    private final OpenIdResolver openIdResolver;

    /**
     * 核心测算接口。同 openid + 同生日 + 同一天 -> 相同 score（幂等）
     */
    @PostMapping("/calc")
    public ApiResponse<FortuneResp> calc(@RequestBody @Valid FortuneCalcReq req) {
        String openid = openIdResolver.currentOpenId();
        log.info("[fortune.calc] openid={}, birthday={}, gender={}", openid, req.getBirthday(), req.getGender());
        FortuneResp resp = fortuneService.calc(req, openid);
        return ApiResponse.success(resp);
    }

    /**
     * 历史列表，分页
     */
    @GetMapping("/history")
    public ApiResponse<HistoryListResp> history(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int pageSize) {
        String openid = openIdResolver.currentOpenId();
        HistoryListResp resp = historyService.page(openid, page, pageSize);
        return ApiResponse.success(resp);
    }

    /**
     * 删除单条历史
     * 线上风险：必须做 openid 归属校验，避免越权删除（已在 HistoryService.deleteOne 内做）
     */
    @DeleteMapping("/history/{id}")
    public ApiResponse<Map<String, Integer>> deleteOne(@PathVariable("id") String recordId) {
        String openid = openIdResolver.currentOpenId();
        int deleted = historyService.deleteOne(openid, recordId);
        Map<String, Integer> data = new HashMap<>();
        data.put("deleted", deleted);
        return ApiResponse.success(data);
    }

    /**
     * 清空当前用户全部历史
     * 线上风险：本期内存方案重启数据丢失属预期；接 DB 后必须改"软删除"
     */
    @DeleteMapping("/history")
    public ApiResponse<Map<String, Integer>> deleteAll() {
        String openid = openIdResolver.currentOpenId();
        int deleted = historyService.deleteAll(openid);
        Map<String, Integer> data = new HashMap<>();
        data.put("deleted", deleted);
        return ApiResponse.success(data);
    }

    /**
     * 看完激励视频后解锁深度内容
     * 线上风险：1) adToken 后端验签防作弊；2) 单 openid 单日解锁次数上限；3) 解锁状态持久化
     */
    @PostMapping("/unlock")
    public ApiResponse<UnlockResp> unlock(@RequestBody @Valid UnlockReq req) {
        String openid = openIdResolver.currentOpenId();
        log.info("[fortune.unlock] openid={}, recordId={}", openid, req.getRecordId());
        UnlockResp resp = fortuneService.unlock(req, openid);
        return ApiResponse.success(resp);
    }
}
