package com.codewalnut.resolvehub.domain;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TagNormalizer {

    private static final int MAXIMUM_TAG_COUNT = 5;

    public Set<String> normalize(Collection<String> tags) {
        if (tags == null) {
            return Set.of();
        }
        if (tags.size() > MAXIMUM_TAG_COUNT) {
            throw new IllegalArgumentException("At most 5 tags are allowed");
        }
        LinkedHashSet<String> normalizedTags = tags.stream()
                .map(String::trim)
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return Collections.unmodifiableSet(normalizedTags);
    }
}
