package com.fortune.dto.req;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 运势计算请求
 */
@Data
public class FortuneCalcReq {

    /** 公历生日，yyyy-MM-dd */
    @NotBlank(message = "生日不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "生日格式应为 yyyy-MM-dd")
    private String birthday;

    /** 出生小时 0-23，未知传 -1 */
    @Min(value = -1, message = "出生小时范围非法")
    @Max(value = 23, message = "出生小时范围非法")
    private Integer birthHour = -1;

    /** 性别 M/F/U */
    @NotBlank(message = "性别不能为空")
    @Pattern(regexp = "^[MFU]$", message = "性别只能为 M/F/U")
    private String gender;

    /** 性格/关注标签，最多 4 个 */
    @Size(max = 4, message = "标签最多 4 个")
    private List<String> tags;

    /** 历法类型 solar/lunar，默认 solar */
    @Pattern(regexp = "^(solar|lunar)$", message = "历法类型只能为 solar/lunar")
    private String calendarType = "solar";
}
