package com.smartnotes.exception;

import com.smartnotes.dto.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final String detail;

    public BusinessException(ErrorCode code) {
        super(code.getMessage());
        this.code = code;
        this.detail = null;
    }

    public BusinessException(ErrorCode code, String detail) {
        super(code.getMessage() + (detail != null ? ": " + detail : ""));
        this.code = code;
        this.detail = detail;
    }

    public int getStatus() {
        return code.getCode();
    }
}
