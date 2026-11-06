package com.algorycarservice.common.exception;

import lombok.Getter;

/** Business error that maps to an {@link ErrorCode}; the user-facing text is resolved from the message bundle. */
@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String messageKey;
    private final transient Object[] messageArgs;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.messageKey());
    }

    protected ApiException(ErrorCode errorCode, String messageKey, Object... messageArgs) {
        super(messageKey);
        this.errorCode = errorCode;
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }
}
