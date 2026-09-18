package com.codewalnut.resolvehub.domain;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.UUID;

@Component
public class UuidReferenceNumberGenerator implements ReferenceNumberGenerator {

    @Override
    public String generate() {
        return "RH-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
    }
}
