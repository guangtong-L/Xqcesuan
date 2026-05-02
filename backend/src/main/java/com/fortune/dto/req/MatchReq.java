package com.fortune.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 星座匹配请求
 */
@Data
public class MatchReq {

    @NotBlank(message = "我的星座不能为空")
    private String zodiacA;

    @NotBlank(message = "TA 的星座不能为空")
    private String zodiacB;
}
