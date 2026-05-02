package com.fortune.dto.req;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * 八字流年请求
 */
@Data
public class BaziReq {

    @NotBlank(message = "生日不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 yyyy-MM-dd")
    private String birthday;

    /** 性别 M/F/U */
    @Pattern(regexp = "^[MFU]$", message = "性别只能为 M/F/U")
    private String gender = "U";
}
