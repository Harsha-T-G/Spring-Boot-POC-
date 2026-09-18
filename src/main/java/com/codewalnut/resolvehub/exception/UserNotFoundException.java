package com.codewalnut.resolvehub.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String identifier) {
        super("User " + identifier + " was not found");
    }
}
