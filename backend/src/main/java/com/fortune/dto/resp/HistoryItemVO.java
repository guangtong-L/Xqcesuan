package com.fortune.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 历史列表单项（精简字段，不含 dimensions/yi/ji）
 */
@Data
@Builder
public class HistoryItemVO {
    private String recordId;
    private String date;
    private String zodiac;
    private Integer score;
    private String level;
    private String summary;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Shanghai")
    private OffsetDateTime createdAt;
}
