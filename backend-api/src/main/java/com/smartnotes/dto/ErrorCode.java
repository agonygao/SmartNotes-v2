package com.smartnotes.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // General
    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // Auth
    USER_ALREADY_EXISTS(1001, "用户名已存在"),
    INVALID_CREDENTIALS(1002, "用户名或密码错误"),
    TOKEN_EXPIRED(1003, "令牌已过期"),
    TOKEN_INVALID(1004, "令牌无效"),
    ACCOUNT_DISABLED(1005, "账号已被禁用"),

    // Notes
    NOTE_NOT_FOUND(2001, "笔记不存在"),
    NOTE_ACCESS_DENIED(2002, "无权访问此笔记"),

    // Word Books
    WORD_BOOK_NOT_FOUND(3001, "词书不存在"),
    WORD_NOT_FOUND(3002, "单词不存在"),
    DUPLICATE_WORD(3003, "单词已存在"),

    // Documents
    DOCUMENT_NOT_FOUND(4001, "文档不存在"),
    UNSUPPORTED_FILE_TYPE(4002, "不支持的文件类型"),
    FILE_TOO_LARGE(4003, "文件过大"),
    FILE_UPLOAD_ERROR(4004, "文件上传失败"),

    // Sync
    SYNC_CONFLICT(5001, "同步冲突"),
    SYNC_CURSOR_INVALID(5002, "同步游标无效");

    private final int code;
    private final String message;
}
