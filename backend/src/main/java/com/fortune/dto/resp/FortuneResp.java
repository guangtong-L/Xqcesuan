package com.fortune.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 运势计算响应（同时也用于历史详情）
 */
@Data
@Builder
public class FortuneResp {

    private String recordId;

    /** yyyy-MM-dd */
    private String date;

    private String zodiac;

    /** 1-100 */
    private Integer score;

    /** 大吉 / 吉 / 平 / 凶（本期凶下沉到平） */
    private String level;

    private String summary;

    /** 固定顺序 love → career → wealth → health */
    private List<DimensionVO> dimensions;

    private List<String> yi;

    private List<String> ji;

    private LuckyVO lucky;

    private Boolean hasDeepContent;

    private Boolean unlocked;

    /** 解锁后填充的深度内容；未解锁为 null */
    private DeepContentVO deepContent;

    private String tips;

    /** 创建时间，历史列表使用 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Shanghai")
    private OffsetDateTime createdAt;
}
