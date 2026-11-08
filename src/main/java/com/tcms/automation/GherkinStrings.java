package com.tcms.automation;

import tools.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** The same quoted string grammar is used by authoring, imports and Cucumber execution. */
public final class GherkinStrings {
    public static final String QUOTED = "\"((?:\\\\.|[^\"\\\\])*)\"";
    public static final String ARGUMENT = "(?:" + QUOTED + "|'((?:\\\\.|[^'\\\\])*)'|(\\S+))";
    private static final ObjectMapper JSON = new ObjectMapper();

    private GherkinStrings() {}

    public static String escape(String value) {
        String quoted = JSON.writeValueAsString(value == null ? "" : value);
        return quoted.substring(1, quoted.length() - 1);
    }

    public static String redact(String text, Collection<String> secrets) {
        if (text == null || secrets.isEmpty()) return text;
        String literals = secrets.stream().filter(secret -> secret != null && !secret.isBlank())
                .flatMap(secret -> Stream.of(secret, escape(secret), escape(secret).replace("\\\"", "\"").replace("'", "\\'")))
                .distinct().sorted(Comparator.comparingInt(String::length).reversed()).map(Pattern::quote)
                .reduce((left, right) -> left + "|" + right).orElse("");
        if (literals.isEmpty()) return text;
        Matcher matches = Pattern.compile("(\\$\\{[a-zA-Z0-9_.-]+\\})|(?:" + literals + ")").matcher(text);
        StringBuilder result = new StringBuilder();
        while (matches.find()) matches.appendReplacement(result, Matcher.quoteReplacement(matches.group(1) != null ? matches.group() : "[REDACTED]"));
        matches.appendTail(result);
        return result.toString();
    }

    public static String decode(String escaped) {
        if (escaped == null) return null;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < escaped.length(); i++) {
            char current = escaped.charAt(i);
            if (current != '\\' || i + 1 == escaped.length()) { result.append(current); continue; }
            char next = escaped.charAt(++i);
            switch (next) {
                case '\\', '"', '\'' -> result.append(next);
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'u' -> {
                    if (i + 4 >= escaped.length()) throw new IllegalArgumentException("Incomplete Unicode escape in step");
                    result.append((char) Integer.parseInt(escaped.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> result.append('\\').append(next);
            }
        }
        return result.toString();
    }
}
