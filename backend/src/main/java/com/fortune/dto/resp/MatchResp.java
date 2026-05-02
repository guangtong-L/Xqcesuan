package com.fortune.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 星座匹配响应
 */
@Data
@Builder
public class MatchResp {
    private String zodiacA;
    private String zodiacB;
    /** 匹配度 40-100（避免极端低分给负面情绪） */
    private Integer score;
    /** 等级：绝配 / 合拍 / 中等 / 待磨合 */
    private String level;
    /** 整体描述 */
    private String summary;
    /** 爱情维度文案 */
    private String loveText;
    /** 相处建议 */
    private String tipText;
    /** 免责提示 */
    private String tips;
}
