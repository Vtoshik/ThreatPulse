package com.threatpulse.common.domain;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * One canonical spelling for technology names.
 * <p>
 * An AI model answers "Spring Framework" or "node.js" while a user types "spring-boot"
 * or "nodejs". Alert rules match technologies by exact comparison, so every name is
 * brought to the same form before it is stored or compared:
 * trimmed, lower case, spaces and underscores replaced by one hyphen, plus a short list
 * of well-known aliases.
 * <p>
 * The SQL in the migration V12 does the same for rows that already exist, keep both in step.
 */
public final class TechnologyNames {
    private static final int MAX_LENGTH = 100;

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("node.js", "nodejs"),
            Map.entry("node", "nodejs"),
            Map.entry("postgres", "postgresql"),
            Map.entry("k8s", "kubernetes"),
            Map.entry("vue.js", "vue"),
            Map.entry("vuejs", "vue"),
            Map.entry("react.js", "react"),
            Map.entry("reactjs", "react"),
            Map.entry("js", "javascript")
    );

    private TechnologyNames() {
    }

    /**
     * @return the canonical name, or an empty string when nothing usable is left
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        String result = name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        result = ALIASES.getOrDefault(result, result);
        return result.length() > MAX_LENGTH ? result.substring(0, MAX_LENGTH) : result;
    }

    /**
     * Normalizes every name, drops empty ones and duplicates, and keeps the order.
     * A null collection gives an empty set.
     */
    public static Set<String> normalizeAll(Collection<String> names) {
        Set<String> result = new LinkedHashSet<>();
        if (names == null) {
            return result;
        }
        for (String name : names) {
            String normalized = normalize(name);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return result;
    }

    public static String[] normalizeAll(String[] names) {
        if (names == null) {
            return null;
        }
        return normalizeAll(List.of(names)).toArray(new String[0]);
    }
}
