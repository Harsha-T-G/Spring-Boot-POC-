package com.codewalnut.resolvehub.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import jakarta.validation.Validation;
import com.codewalnut.resolvehub.domain.TicketPriority;
import com.codewalnut.resolvehub.domain.TicketStatus;
import static org.assertj.core.api.Assertions.assertThat;

class TicketRequestValidationTest {
    @ParameterizedTest
    @CsvSource({"4,19,101,false", "5,20,100,true", "120,1000,100,true", "121,1001,101,false"})
    void givenUnicodeLengths_whenIntakeValidated_thenMatchDatabaseCharacterLimits(
            int titleLength, int descriptionLength, int tagLength, boolean valid) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new CreateTicketRequest("😀".repeat(titleLength), "😀".repeat(descriptionLength),
                    TicketPriority.HIGH, java.util.List.of("😀".repeat(tagLength)), null);
            var errors = factory.getValidator().validate(request);
            if (valid) {
                assertThat(errors).isEmpty();
            } else {
                assertThat(errors).anyMatch(error -> error.getPropertyPath().toString().equals("title"));
                assertThat(errors).anyMatch(error -> error.getPropertyPath().toString().equals("description"));
                assertThat(errors).anyMatch(error -> error.getPropertyPath().toString().startsWith("tags"));
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"19,false", "20,true", "500,true", "501,false"})
    void givenUnicodeSummary_whenValidated_thenMatchDatabaseCharacterLimits(int length, boolean valid) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new UpdateTicketStatusRequest(TicketStatus.RESOLVED, "😀".repeat(length));
            assertThat(factory.getValidator().validate(request).isEmpty()).isEqualTo(valid);
        }
    }

    @Test
    void givenExpandingUnicodeTag_whenValidated_thenRejectNormalizedLength() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new CreateTicketRequest("Valid title", "A sufficiently detailed description",
                    TicketPriority.HIGH, java.util.List.of("İ".repeat(100)), null);
            assertThat(factory.getValidator().validate(request))
                    .anyMatch(error -> error.getPropertyPath().toString().startsWith("tags"));
        }
    }

    @Test
    void givenMutableTags_whenRequestConstructed_thenDefensivelyCopyAndNormalize() {
        var tags = new java.util.ArrayList<>(java.util.List.of(" Java "));
        var request = new CreateTicketRequest("Valid title", "A sufficiently detailed description",
                TicketPriority.HIGH, tags, null);
        tags.clear();
        assertThat(request.tags()).containsExactly("java");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> request.tags().add("extra"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void givenPaddedShortTitle_whenValidated_thenRejectNormalizedLength() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new CreateTicketRequest("    hi    ", "A sufficiently detailed description",
                    TicketPriority.HIGH, null, null);
            assertThat(factory.getValidator().validate(request))
                    .anyMatch(error -> error.getPropertyPath().toString().equals("title"));
        }
    }

    @Test
    void givenPaddedShortSummary_whenValidated_thenRejectNormalizedLength() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var request = new UpdateTicketStatusRequest(TicketStatus.RESOLVED, "                    short ");
            assertThat(factory.getValidator().validate(request)).isNotEmpty();
        }
    }
}
