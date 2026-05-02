package com.fortune.service;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortune.common.BizException;
import com.fortune.common.ErrorCode;
import com.fortune.dto.req.MatchReq;
import com.fortune.dto.resp.MatchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * 星座匹配（mock）
 * <p>
 * 算法：score = (MD5(zodiacA + zodiacB)) % 60 + 40，避免极端低分给负面情绪。
 * 顺序无关：a+b 与 b+a 计算时先排序，确保对称。
 */
@Slf4j
@Service
public class MatchService {

    private final ObjectMapper om;
    private List<String> summaryPool;
    private List<String> lovePool;
    private List<String> tipPool;

    private static final String TIPS = "本结果基于趣味算法生成，仅供娱乐参考。";

    public MatchService(@Qualifier("businessObjectMapper") ObjectMapper om) {
        this.om = om;
    }

    @PostConstruct
    public void init() {
        try {
            String json = ResourceUtil.readUtf8Str("mock/match.json");
            //noinspection unchecked
            Map<String, Object> root = om.readValue(json, Map.class);
            //noinspection unchecked
            this.summaryPool = (List<String>) root.get("summary");
            //noinspection unchecked
            this.lovePool = (List<String>) root.get("love");
            //noinspection unchecked
            this.tipPool = (List<String>) root.get("tip");
            log.info("Match mock 数据加载完成 summary={} love={} tip={}",
                    summaryPool.size(), lovePool.size(), tipPool.size());
        } catch (Exception e) {
            throw new IllegalStateException("Match mock 加载失败", e);
        }
    }

    public MatchResp match(MatchReq req) {
        String a = norm(req.getZodiacA());
        String b = norm(req.getZodiacB());
        if (StrUtil.isAllBlank(a, b)) {
            throw new BizException(ErrorCode.PARAM_MISSING);
        }
        // 对称：保证 a+b 与 b+a 同分
        String key = a.compareTo(b) <= 0 ? a + "_" + b : b + "_" + a;
        byte[] seed = DigestUtil.md5(key);
        int s0 = absInt(seed, 0);
        int s1 = absInt(seed, 4);
        int s2 = absInt(seed, 8);

        int score = s0 % 61 + 40;  // 40-100
        String level = level(score);

        return MatchResp.builder()
                .zodiacA(a)
                .zodiacB(b)
                .score(score)
                .level(level)
                .summary(pick(summaryPool, s0))
                .loveText(pick(lovePool, s1))
                .tipText(pick(tipPool, s2))
                .tips(TIPS)
                .build();
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim();
    }

    private static String level(int score) {
        if (score >= 90) return "绝配";
        if (score >= 75) return "合拍";
        if (score >= 60) return "中等";
        return "待磨合";
    }

    private static int absInt(byte[] b, int from) {
        int v = 0;
        for (int i = from; i < Math.min(from + 4, b.length); i++) v = (v << 8) | (b[i] & 0xFF);
        return v & 0x7FFFFFFF;
    }

    private static <T> T pick(List<T> pool, int seed) {
        return pool.get(Math.floorMod(seed, pool.size()));
    }
}
