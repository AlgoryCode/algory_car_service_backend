package com.algorycarservice.common.web;

import com.algorycarservice.common.exception.ApiError;
import com.algorycarservice.common.exception.ErrorCode;
import com.algorycarservice.common.i18n.Messages;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.LocaleResolver;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/** Writes an {@link ApiError} body from servlet filters, which run before {@code GlobalExceptionHandler} applies. */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final JsonMapper jsonMapper;
    private final Messages messages;
    private final LocaleResolver localeResolver;

    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code) throws IOException {
        // Outside DispatcherServlet the request locale is not bound yet, so it is resolved here.
        String message = messages.get(localeResolver.resolveLocale(request), code.messageKey());
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), ApiError.of(code, message, request.getRequestURI()));
    }
}
