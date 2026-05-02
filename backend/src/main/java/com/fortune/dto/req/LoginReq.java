package com.fortune.dto.req;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录请求
 */
@Data
public class LoginReq {

    /** 微信小程序 wx.login 返回的临时 code */
    @NotBlank(message = "code 不能为空")
    @Size(max = 64, message = "code 长度非法")
    private String code;
}
