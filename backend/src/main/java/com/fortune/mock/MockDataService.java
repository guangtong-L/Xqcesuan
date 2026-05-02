package com.fortune.mock;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortune.common.BizException;
import com.fortune.common.ErrorCode;
import com.fortune.dto.resp.DeepContentVO;
import com.fortune.dto.resp.LuckyVO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mock 文案池加载与抽取
 * <p>
 * 设计要点：
 * - 启动期 @PostConstruct 一次性加载，运行期只读，天然线程安全
 * - 任何 JSON 缺字段一律 fail-fast，避免线上随机 NPE
 */
@Slf4j
@Component
public class MockDataService {

    /** level → dimension → text[] */
    @Getter
    private Map<String, Map<String, List<String>>> fortuneText;

    @Getter
    private List<Map<String, String>> colors;

    @Getter
    private List<String> directions;

    @Getter
    private List<String> yiPool;

    @Getter
    private List<String> jiPool;

    @Getter
    private DeepContentVO deepContentTemplate;

    private final ObjectMapper om;

    public MockDataService(@Qualifier("businessObjectMapper") ObjectMapper om) {
        this.om = om;
    }

    @PostConstruct
    public void init() {
        try {
            this.fortuneText = parseFortuneText();
            Map<String, Object> luckyRoot = parseJson("mock/lucky_pool.json", Map.class);
            //noinspection unchecked
            this.colors = (List<Map<String, String>>) luckyRoot.get("colors");
            //noinspection unchecked
            this.directions = (List<String>) luckyRoot.get("directions");

            Map<String, Object> yjRoot = parseJson("mock/yi_ji_pool.json", Map.class);
            //noinspection unchecked
            this.yiPool = (List<String>) yjRoot.get("yi");
            //noinspection unchecked
            this.jiPool = (List<String>) yjRoot.get("ji");

            this.deepContentTemplate = parseJson("mock/deep_content.json", DeepContentVO.class);

            // 校验，避免线上偶发 NPE
            assertNotEmpty(colors, "lucky_pool.colors");
            assertNotEmpty(directions, "lucky_pool.directions");
            assertNotEmpty(yiPool, "yi_ji_pool.yi");
            assertNotEmpty(jiPool, "yi_ji_pool.ji");
            Objects.requireNonNull(deepContentTemplate, "deep_content.json 解析为空");

            log.info("Mock 数据加载完成 levels={} colors={} directions={} yi={} ji={}",
                    fortuneText.keySet(), colors.size(), directions.size(), yiPool.size(), jiPool.size());
        } catch (Exception e) {
            log.error("Mock 数据加载失败", e);
            throw new IllegalStateException("Mock 数据加载失败：" + e.getMessage(), e);
        }
    }

    private Map<String, Map<String, List<String>>> parseFortuneText() throws Exception {
        try (InputStream in = ResourceUtil.getStream("mock/fortune_text.json")) {
            String json = IoUtil.read(in, StandardCharsets.UTF_8);
            //noinspection unchecked
            return om.readValue(json, Map.class);
        }
    }

    private <T> T parseJson(String path, Class<T> clz) throws Exception {
        try (InputStream in = ResourceUtil.getStream(path)) {
            String json = IoUtil.read(in, StandardCharsets.UTF_8);
            return om.readValue(json, clz);
        }
    }

    private void assertNotEmpty(List<?> list, String name) {
        if (list == null || list.isEmpty()) {
            throw new IllegalStateException("Mock 数据缺失：" + name);
        }
    }

    // ====== 抽取方法 ======

    /**
     * 按 level + dimension 取一条文案
     *
     * @param level     level_excellent / level_good / level_normal
     * @param dimension love / career / wealth / health
     * @param seedInt   非负 seed，用于稳定取
     */
    public String pickFortuneText(String level, String dimension, int seedInt) {
        Map<String, List<String>> bucket = fortuneText.get(level);
        if (bucket == null) {
            // level 缺失下沉到 level_normal，避免 NPE
            bucket = fortuneText.get("level_normal");
        }
        List<String> list = bucket == null ? null : bucket.get(dimension);
        if (list == null || list.isEmpty()) {
            return "今天是平和的一天，慢慢来。";
        }
        int idx = Math.floorMod(seedInt, list.size());
        return list.get(idx);
    }

    /**
     * 取幸运三件套
     */
    public LuckyVO pickLucky(int seedInt) {
        if (colors.isEmpty() || directions.isEmpty()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "幸运池为空");
        }
        Map<String, String> color = colors.get(Math.floorMod(seedInt, colors.size()));
        int number = Math.floorMod(seedInt >>> 1, 9) + 1;
        String dir = directions.get(Math.floorMod(seedInt >>> 2, directions.size()));
        return LuckyVO.builder()
                .color(color.get("name"))
                .colorHex(color.get("hex"))
                .number(number)
                .direction(dir)
                .build();
    }

    /**
     * 抽宜忌：宜 3-5、忌 1-3，保证宜多于忌
     *
     * @return [yiList, jiList]
     */
    public List<List<String>> pickYiJi(int seedInt) {
        int seed = Math.abs(seedInt);
        int yiCount = 3 + (seed % 3);          // 3~5
        int jiCount = 1 + ((seed >>> 3) % 3);  // 1~3
        List<String> yi = pickN(yiPool, yiCount, seed);
        List<String> ji = pickN(jiPool, jiCount, seed * 31 + 7);
        return java.util.Arrays.asList(yi, ji);
    }

    private List<String> pickN(List<String> pool, int n, int seed) {
        if (pool == null || pool.isEmpty()) {
            return Collections.emptyList();
        }
        int size = pool.size();
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        int probe = Math.floorMod(seed, size);
        int safety = 0;
        while (result.size() < n && safety++ < size * 2) {
            result.add(pool.get(probe));
            probe = Math.floorMod(probe + 7, size); // 步长互质，分布更均匀
        }
        return new java.util.ArrayList<>(result);
    }

    /**
     * 拷贝一份 deepContent，避免外部修改单例
     */
    public DeepContentVO cloneDeepContent() {
        DeepContentVO t = deepContentTemplate;
        return DeepContentVO.builder()
                .monthTrend(t.getMonthTrend())
                .yearKeyword(t.getYearKeyword() == null ? null : new java.util.ArrayList<>(t.getYearKeyword()))
                .advice(t.getAdvice() == null ? null : new java.util.ArrayList<>(t.getAdvice()))
                .compatibleZodiac(t.getCompatibleZodiac() == null ? null : new java.util.ArrayList<>(t.getCompatibleZodiac()))
                .build();
    }

    /**
     * 工具：根据 score 选 level key
     */
    public static String levelKeyByScore(int score) {
        if (score >= 90) return "level_excellent";
        if (score >= 70) return "level_good";
        return "level_normal"; // 40~69 与 <40 共用，避开"凶"
    }

    /**
     * 工具：score → 等级中文展示
     */
    public static String levelDisplayByScore(int score) {
        if (score >= 90) return "大吉";
        if (score >= 70) return "吉";
        return "平";
    }

    /**
     * 工具：给个非空检查方法，避免外部直接 NPE
     */
    public boolean isReady() {
        return fortuneText != null && !fortuneText.isEmpty()
                && colors != null && directions != null
                && yiPool != null && jiPool != null;
    }
}
