package com.codewalnut.resolvehub.exception;

public class DisabledUserException extends RuntimeException {

    public DisabledUserException() {
        super("The selected customer is disabled");
    }
}
