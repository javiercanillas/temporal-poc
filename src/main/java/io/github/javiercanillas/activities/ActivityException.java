package io.github.javiercanillas.activities;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ActivityException extends RuntimeException {

    private final String code;

    public ActivityException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ActivityException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
