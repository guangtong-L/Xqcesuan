package com.fortune.service;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortune.common.BizException;
import com.fortune.common.ErrorCode;
import com.fortune.dto.resp.SignResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 幸运签（mock）
 * <p>
 * 算法：MD5(openid + 当日 yyyy-MM-dd) → seed
 *  - 签号：seed % 50 + 1
 *  - 签名：按权重抽（上上签 10% / 上签 25% / 中签 55% / 下签 10%）
 *  - 签诗、解读、宜忌从池子里取
 * 同一人当日结果一致；跨日刷新。
 */
@Slf4j
@Service
public class SignService {

    private final ObjectMapper om;
    private List<String> names;
    private List<Integer> namesWeight;
    private List<List<String>> poems;
    private List<String> explains;
    private List<String> yiPool;
    private List<String> jiPool;

    private static final String TIPS = "本签由趣味算法生成，仅供娱乐参考。";
    private static final DateTimeFormatter D_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public SignService(@Qualifier("businessObjectMapper") ObjectMapper om) {
        this.om = om;
    }

    @PostConstruct
    public void init() {
        try {
            String json = ResourceUtil.readUtf8Str("mock/sign.json");
            //noinspection unchecked
            Map<String, Object> root = om.readValue(json, Map.class);
            //noinspection unchecked
            this.names = (List<String>) root.get("names");
            //noinspection unchecked
            this.namesWeight = (List<Integer>) root.get("namesWeight");
            //noinspection unchecked
            this.poems = (List<List<String>>) root.get("poems");
            //noinspection unchecked
            this.explains = (List<String>) root.get("explain");
            //noinspection unchecked
            this.yiPool = (List<String>) root.get("yi");
            //noinspection unchecked
            this.jiPool = (List<String>) root.get("ji");
            log.info("Sign mock 数据加载完成 poems={} explains={} yi={} ji={}",
                    poems.size(), explains.size(), yiPool.size(), jiPool.size());
        } catch (Exception e) {
            throw new IllegalStateException("Sign mock 加载失败", e);
        }
    }

    public SignResp draw(String openid) {
        if (StrUtil.isBlank(openid)) {
            throw new BizException(ErrorCode.NOT_LOGIN);
        }
        String today = LocalDate.now().format(D_FMT);
        byte[] seed = DigestUtil.md5(openid + today);
        int s0 = absInt(seed, 0);
        int s1 = absInt(seed, 4);
        int s2 = absInt(seed, 8);
        int s3 = absInt(seed, 12);

        int signNo = s0 % 50 + 1;
        String signName = pickWeighted(names, namesWeight, s0);
        List<String> poem = pick(poems, s1);
        String explain = pick(explains, s2);
        List<String> yi = pickN(yiPool, 3, s3);
        List<String> ji = pickN(jiPool, 2, s3 * 31 + 7);

        return SignResp.builder()
                .signNo(signNo)
                .signName(signName)
                .poem(poem)
                .explain(explain)
                .yi(yi)
                .ji(ji)
                .date(today)
                .tips(TIPS)
                .build();
    }

    private static <T> T pick(List<T> pool, int seed) {
        return pool.get(Math.floorMod(seed, pool.size()));
    }

    private static String pickWeighted(List<String> names, List<Integer> weights, int seed) {
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        int r = Math.floorMod(seed, total);
        int acc = 0;
        for (int i = 0; i < weights.size(); i++) {
            acc += weights.get(i);
            if (r < acc) return names.get(i);
        }
        return names.get(names.size() - 1);
    }

    private static List<String> pickN(List<String> pool, int n, int seed) {
        int size = pool.size();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        int probe = Math.floorMod(seed, size);
        int safety = 0;
        while (result.size() < n && safety++ < size * 2) {
            result.add(pool.get(probe));
            probe = Math.floorMod(probe + 7, size);
        }
        return new ArrayList<>(result);
    }

    private static int absInt(byte[] b, int from) {
        int v = 0;
        for (int i = from; i < Math.min(from + 4, b.length); i++) v = (v << 8) | (b[i] & 0xFF);
        return v & 0x7FFFFFFF;
    }
}
