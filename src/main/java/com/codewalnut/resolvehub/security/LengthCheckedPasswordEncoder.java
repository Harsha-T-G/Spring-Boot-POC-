package com.codewalnut.resolvehub.security;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;

public final class LengthCheckedPasswordEncoder implements PasswordEncoder {
    private static final int MAXIMUM_PASSWORD_BYTES = 72;
    private final PasswordEncoder delegate;

    public LengthCheckedPasswordEncoder(PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        if (!hasValidLength(rawPassword)) {
            throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes");
        }
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        boolean validLength = hasValidLength(rawPassword);
        boolean matched = delegate.matches(validLength ? rawPassword : "invalid-overlong-password", encodedPassword);
        return validLength && matched;
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return delegate.upgradeEncoding(encodedPassword);
    }

    private boolean hasValidLength(CharSequence password) {
        return password != null && password.toString().getBytes(StandardCharsets.UTF_8).length <= MAXIMUM_PASSWORD_BYTES;
    }
}
