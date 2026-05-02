package com.fortune.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 深度内容
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeepContentVO {
    private String monthTrend;
    private List<String> yearKeyword;
    private List<String> advice;
    private List<String> compatibleZodiac;
}
