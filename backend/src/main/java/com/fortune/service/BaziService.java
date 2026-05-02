package com.fortune.service;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortune.common.BizException;
import com.fortune.common.ErrorCode;
import com.fortune.dto.req.BaziReq;
import com.fortune.dto.resp.BaziResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 八字流年（mock）
 * <p>
 * 算法：MD5(openid + birthday + 当年 yyyy) → seed → 从 mock 池稳定抽取
 * 同一人同一年结果一致；跨年自动变化。
 */
@Slf4j
@Service
public class BaziService {

    private final ObjectMapper businessObjectMapper;
    private static final String TIPS = "本结果基于趣味算法生成，仅供娱乐参考，不构成任何决策建议。";

    private List<String> yearOverviews;
    private Map<String, List<String>> seasonText;
    private List<List<String>> keywordsPool;
    private List<String> suggestions;

    public BaziService(@Qualifier("businessObjectMapper") ObjectMapper om) {
        this.businessObjectMapper = om;
    }

    @PostConstruct
    public void init() {
        try {
            String json = ResourceUtil.readUtf8Str("mock/bazi.json");
            //noinspection unchecked
            Map<String, Object> root = businessObjectMapper.readValue(json, Map.class);
            //noinspection unchecked
            this.yearOverviews = (List<String>) root.get("yearOverview");
            Map<String, List<String>> seasons = new HashMap<>();
            //noinspection unchecked
            seasons.put("spring", (List<String>) root.get("spring"));
            //noinspection unchecked
            seasons.put("summer", (List<String>) root.get("summer"));
            //noinspection unchecked
            seasons.put("autumn", (List<String>) root.get("autumn"));
            //noinspection unchecked
            seasons.put("winter", (List<String>) root.get("winter"));
            this.seasonText = seasons;
            //noinspection unchecked
            this.keywordsPool = (List<List<String>>) root.get("keywords");
            //noinspection unchecked
            this.suggestions = (List<String>) root.get("suggestions");
            log.info("Bazi mock 数据加载完成，overview={} suggestions={}",
                    yearOverviews.size(), suggestions.size());
        } catch (Exception e) {
            throw new IllegalStateException("Bazi mock 加载失败", e);
        }
    }

    public BaziResp calc(BaziReq req, String openid) {
        if (StrUtil.isBlank(openid)) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        int year = LocalDate.now().getYear();
        byte[] seed = DigestUtil.md5(openid + req.getBirthday() + year);

        int s0 = absInt(seed, 0);
        int s1 = absInt(seed, 4);
        int s2 = absInt(seed, 8);
        int s3 = absInt(seed, 12);

        String overview = pick(yearOverviews, s0);
        List<String> kws = pick(keywordsPool, s1);
        String suggestion = pick(suggestions, s2);

        List<BaziResp.QuarterVO> quarters = Arrays.asList(
                buildQuarter("spring", "春", s0),
                buildQuarter("summer", "夏", s1),
                buildQuarter("autumn", "秋", s2),
                buildQuarter("winter", "冬", s3)
        );

        return BaziResp.builder()
                .year(year)
                .yearOverview(overview)
                .quarters(quarters)
                .keywords(kws)
                .suggestion(suggestion)
                .tips(TIPS)
                .build();
    }

    private BaziResp.QuarterVO buildQuarter(String key, String name, int seed) {
        int score = (seed & 0x7FFFFFFF) % 41 + 60; // 60-100，整体偏正向
        String text = pick(seasonText.getOrDefault(key, Collections.emptyList()), seed);
        return BaziResp.QuarterVO.builder()
                .key(key).name(name).score(score).text(text).build();
    }

    private static int absInt(byte[] b, int from) {
        int v = 0;
        for (int i = from; i < Math.min(from + 4, b.length); i++) v = (v << 8) | (b[i] & 0xFF);
        return v & 0x7FFFFFFF;
    }

    private static <T> T pick(List<T> pool, int seed) {
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(Math.floorMod(seed, pool.size()));
    }
}
