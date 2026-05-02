package com.fortune.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

/**
 * 全局异常兜底。
 * - 业务异常 BizException → 业务码
 * - 参数校验 → 1002
 * - 其他未知 → 5000，不回传堆栈，仅日志
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBiz(BizException e) {
        log.warn("[业务异常] code={} msg={}", e.getCode(), e.getMessage());
        return ApiResponse.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? ErrorCode.PARAM_INVALID.getMessage() : fe.getDefaultMessage();
        log.warn("[参数校验失败] {}", msg);
        return ApiResponse.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBind(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? ErrorCode.PARAM_INVALID.getMessage() : fe.getDefaultMessage();
        log.warn("[参数绑定失败] {}", msg);
        return ApiResponse.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraint(ConstraintViolationException e) {
        String msg = ErrorCode.PARAM_INVALID.getMessage();
        for (ConstraintViolation<?> v : e.getConstraintViolations()) {
            msg = v.getMessage();
            break;
        }
        log.warn("[约束校验失败] {}", msg);
        return ApiResponse.fail(ErrorCode.PARAM_INVALID.getCode(), msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("[缺少必填参数] {}", e.getParameterName());
        return ApiResponse.fail(ErrorCode.PARAM_MISSING.getCode(),
                "缺少必填参数：" + e.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.PARAM_INVALID.getCode(), "请求方法不被允许：" + e.getMethod()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoHandlerFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.RECORD_NOT_FOUND.getCode(), "接口不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknown(Exception e) {
        // 不暴露堆栈到前端，但要在服务端保留完整日志
        log.error("[系统异常] {}", e.getMessage(), e);
        return ApiResponse.fail(ErrorCode.SYSTEM_ERROR);
    }
}
