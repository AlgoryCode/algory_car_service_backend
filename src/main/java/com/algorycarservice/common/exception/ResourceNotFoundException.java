package com.algorycarservice.common.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;

public class ResourceNotFoundException extends ApiException {

    /** @param resource key suffix under {@code resource.*}, e.g. {@code vehicle} */
    public ResourceNotFoundException(String resource) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                ErrorCode.RESOURCE_NOT_FOUND.messageKey() + ".detail",
                new DefaultMessageSourceResolvable("resource." + resource));
    }
}
