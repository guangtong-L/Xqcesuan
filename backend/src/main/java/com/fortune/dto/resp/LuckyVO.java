package com.fortune.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 幸运三件套
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LuckyVO {
    private String color;
    private String colorHex;
    /** 1-9 */
    private Integer number;
    /** 8 方位 */
    private String direction;
}
