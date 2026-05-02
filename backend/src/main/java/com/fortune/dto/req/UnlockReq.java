package com.fortune.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 激励视频解锁请求
 * <p>
 * TODO 上线前 adToken 必须做微信广告回调验签，否则会被刷
 */
@Data
public class UnlockReq {

    @NotBlank(message = "记录 id 不能为空")
    @Size(max = 64, message = "记录 id 长度非法")
    private String recordId;

    /** 微信激励视频回调 token；本期不验签 */
    @NotBlank(message = "广告 token 不能为空")
    @Size(max = 256, message = "广告 token 长度非法")
    private String adToken;
}
