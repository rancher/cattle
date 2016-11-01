package io.cattle.platform.docker.client;

import java.util.regex.Pattern;

public class Regexp {

    public static final Pattern ALPHA_NUMERIC_REGEXP = Pattern.compile("[a-z0-9]+");

    public static final Pattern SEPARATOR_REGEXP = Pattern.compile("(?:[._]|__|[-]*)");

    public static final Pattern NAME_COMPONENT_REGEXP = expression(ALPHA_NUMERIC_REGEXP,
            optional(repeated(SEPARATOR_REGEXP, ALPHA_NUMERIC_REGEXP)));

    public static final Pattern HOSTNAME_COMPONENT_REGEXP = Pattern
            .compile("(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])");

    public static final Pattern HOSTNAME_REGEXP = expression(HOSTNAME_COMPONENT_REGEXP,
            optional(repeated(literal("."), HOSTNAME_COMPONENT_REGEXP)),
            optional(literal(":"), Pattern.compile("[0-9]+")));

    public static final Pattern TAG_REGEXP = Pattern.compile("[\\w][\\w.-]{0,127}");

    public static final Pattern ANCHORED_TAG_REGEXP = anchored(TAG_REGEXP);

    public static final Pattern DIGEST_REGEXP = Pattern
            .compile("[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*[:][0-9A-Fa-f]{32,}");

    public static final Pattern ANCHORED_DIGEST_REGEXP = anchored(DIGEST_REGEXP);

    public static final Pattern NAME_REGEXP = expression(optional(HOSTNAME_REGEXP, literal("/")), NAME_COMPONENT_REGEXP,
            optional(repeated(literal("/"), NAME_COMPONENT_REGEXP)));

    public static final Pattern ANCHORED_NAME_REGEXP = anchored(optional(capture(HOSTNAME_REGEXP), literal("/")),
            capture(NAME_COMPONENT_REGEXP, optional(repeated(literal("/"), NAME_COMPONENT_REGEXP))));

    public static final Pattern REFERENCE_REGEXP = anchored(capture(NAME_REGEXP),
            optional(literal(":"), capture(TAG_REGEXP)), optional(literal("@"), capture(DIGEST_REGEXP)));

    public static Pattern literal(String s) {
        return Pattern.compile(Pattern.quote(s));
    }

    public static Pattern expression(Pattern pattern) {
        return pattern;
    }

    public static Pattern expression(Pattern patterns1, Pattern pattern2) {
        String ret = "";
        ret = patterns1.toString() + pattern2.toString();
        return Pattern.compile(ret);
    }

    public static Pattern expression(Pattern patterns1, Pattern pattern2, Pattern pattern3) {
        String ret = "";
        ret = patterns1.toString() + pattern2.toString() + pattern3.toString();
        return Pattern.compile(ret);
    }

    public static Pattern group(Pattern pattern) {
        return Pattern.compile("(?:" + expression(pattern).toString() + ")");
    }

    public static Pattern group(Pattern pattern1, Pattern pattern2) {
        return Pattern.compile("(?:" + expression(pattern1, pattern2).toString() + ")");
    }

    public static Pattern capture(Pattern pattern) {
        return Pattern.compile("(" + expression(pattern).toString() + ")");
    }

    public static Pattern capture(Pattern pattern1, Pattern pattern2) {
        return Pattern.compile("(" + expression(pattern1, pattern2).toString() + ")");
    }

    public static Pattern anchored(Pattern pattern) {
        return Pattern.compile("^" + expression(pattern).toString() + "$");
    }

    public static Pattern anchored(Pattern pattern1, Pattern pattern2) {
        return Pattern.compile("^" + expression(pattern1, pattern2).toString() + "$");
    }

    public static Pattern anchored(Pattern pattern1, Pattern pattern2, Pattern pattern3) {
        return Pattern.compile("^" + expression(pattern1, pattern2, pattern3).toString() + "$");
    }

    public static Pattern optional(Pattern pattern) {
        return Pattern.compile(group(pattern).toString() + "?");
    }

    public static Pattern optional(Pattern pattern1, Pattern pattern2) {
        return Pattern.compile(group(pattern1, pattern2) + "?");
    }

    public static Pattern repeated(Pattern pattern1, Pattern pattern2) {
        return Pattern.compile(group(pattern1, pattern2).toString() + "+");
    }
}
