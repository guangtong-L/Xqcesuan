package com.fortune.dto.req;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 历史查询请求（GET 参数封装，便于扩展）
 */
@Data
public class HistoryQueryReq {

    @Min(value = 1, message = "页码从 1 开始")
    private Integer page = 1;

    @Min(value = 1, message = "每页至少 1 条")
    @Max(value = 50, message = "每页最多 50 条")
    private Integer pageSize = 10;
}
