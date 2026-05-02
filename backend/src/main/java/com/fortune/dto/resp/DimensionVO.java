package com.fortune.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单维度运势
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DimensionVO {
    /** love / career / wealth / health */
    private String key;
    private String name;
    private Integer score;
    private String text;
}
