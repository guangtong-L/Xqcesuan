package com.fortune.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 幸运签响应
 */
@Data
@Builder
public class SignResp {
    /** 签号 1-50 */
    private Integer signNo;
    /** 签名：上上签 / 上签 / 中签 / 下签 */
    private String signName;
    /** 4 句签诗 */
    private List<String> poem;
    /** 解读 */
    private String explain;
    /** 当日宜 */
    private List<String> yi;
    /** 当日忌 */
    private List<String> ji;
    /** 当日日期 yyyy-MM-dd */
    private String date;
    /** 免责提示 */
    private String tips;
}
