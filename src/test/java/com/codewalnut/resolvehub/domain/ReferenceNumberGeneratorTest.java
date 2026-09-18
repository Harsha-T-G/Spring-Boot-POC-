package com.codewalnut.resolvehub.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReferenceNumberGeneratorTest {

    private final ReferenceNumberGenerator referenceNumberGenerator = new UuidReferenceNumberGenerator();

    @Test
    void givenGenerator_whenTwoReferencesAreRequested_thenReturnsDistinctReadableReferences() {
        String firstReference = referenceNumberGenerator.generate();
        String secondReference = referenceNumberGenerator.generate();

        assertTrue(firstReference.matches("RH-[0-9A-F-]{36}"));
        assertTrue(secondReference.matches("RH-[0-9A-F-]{36}"));
        assertNotEquals(firstReference, secondReference);
    }
}
