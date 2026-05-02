package com.fortune.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResp {

    private String openid;

    private String token;

    /** ISO8601 带时区，前端按字符串展示 */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Shanghai")
    private OffsetDateTime expireAt;

    private Boolean isNew;
}
