package com.codewalnut.resolvehub.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TagNormalizerTest {

    private final TagNormalizer tagNormalizer = new TagNormalizer();

    @Test
    void givenMixedCasePaddedAndDuplicateTags_whenNormalized_thenReturnsUniqueLowercaseTags() {
        // Arrange
        List<String> suppliedTags = List.of("  Billing ", "URGENT", "billing", " Api ");

        // Act
        Set<String> result = tagNormalizer.normalize(suppliedTags);

        // Assert
        assertEquals(Set.of("billing", "urgent", "api"), result);
    }

    @Test
    void givenNullTags_whenNormalized_thenReturnsEmptySet() {
        Set<String> result = tagNormalizer.normalize(null);

        assertEquals(Set.of(), result);
    }

    @Test
    void givenMoreThanFiveTags_whenNormalized_thenRejectsInput() {
        // Arrange
        List<String> suppliedTags = List.of("one", "two", "three", "four", "five", "six");

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tagNormalizer.normalize(suppliedTags));
        assertEquals("At most 5 tags are allowed", exception.getMessage());
    }
}
