package io.github.javiercanillas.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public final Error handleException(final HttpServletRequest request,
                                       final HttpServletResponse response, final Exception ex) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return Error.builder()
                .message(ex.getMessage())
                .code("0")
                .build();
    }
}
