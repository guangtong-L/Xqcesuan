package com.fortune.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 历史列表分页响应
 */
@Data
@Builder
public class HistoryListResp {
    private Integer page;
    private Integer pageSize;
    private Integer total;
    private List<HistoryItemVO> list;
}
