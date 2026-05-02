package com.fortune.common;

import lombok.Getter;

/**
 * 业务错误码字典
 * <p>
 * 与接口契约 4.0 节一致。新增请勿复用已有码。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),

    PARAM_MISSING(1001, "必填参数缺失"),
    PARAM_INVALID(1002, "参数格式错误"),
    PARAM_RANGE(1003, "参数取值超出范围"),

    NOT_LOGIN(2001, "未登录或登录已过期"),
    RECORD_NOT_FOUND(2002, "记录不存在"),
    ALREADY_UNLOCKED(2003, "记录已解锁"),

    RATE_LIMITED(4001, "操作过于频繁，请稍后再试"),

    SYSTEM_ERROR(5000, "系统繁忙，请稍后再试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
