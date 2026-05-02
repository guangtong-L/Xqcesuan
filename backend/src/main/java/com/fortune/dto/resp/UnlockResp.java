package com.fortune.dto.resp;

import lombok.Builder;
import lombok.Data;

/**
 * 解锁响应
 */
@Data
@Builder
public class UnlockResp {
    private String recordId;
    private Boolean unlocked;
    private DeepContentVO deepContent;
}
