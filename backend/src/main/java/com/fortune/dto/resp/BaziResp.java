package com.fortune.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 八字流年响应
 */
@Data
@Builder
public class BaziResp {
    /** 当年（yyyy） */
    private Integer year;
    /** 年度综评 */
    private String yearOverview;
    /** 4 季度概览：春/夏/秋/冬 */
    private List<QuarterVO> quarters;
    /** 关键词 3 个 */
    private List<String> keywords;
    /** 1 段建议 */
    private String suggestion;
    /** 免责提示 */
    private String tips;

    @Data
    @Builder
    public static class QuarterVO {
        /** spring / summer / autumn / winter */
        private String key;
        /** 中文名 */
        private String name;
        /** 该季度评分 1-100 */
        private Integer score;
        /** 该季度文案 */
        private String text;
    }
}
