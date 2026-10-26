package com.example.aiagentdemo.common;

/** 业务异常：由 GlobalExceptionHandler 统一转成 Result。 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        this(1, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}