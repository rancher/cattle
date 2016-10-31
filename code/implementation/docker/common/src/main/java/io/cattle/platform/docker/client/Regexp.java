package io.cattle.platform.docker.client;

import java.util.regex.Pattern;

public class Regexp {

    public static final Pattern ALPHANUMERICREGEXP = Pattern.compile("[a-z0-9]+");

    public static final Pattern SEPARATORREGEXP = Pattern.compile("(?:[._]|__|[-]*)");

    public static final Pattern NAMECOMPONENTREGEXP = expression(ALPHANUMERICREGEXP,
            optional(repeated(SEPARATORREGEXP, ALPHANUMERICREGEXP)));

    public static final Pattern HOSTNAMECOMPONENTREGEXP = Pattern
            .compile("(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])");

    public static final Pattern HOSTNAMEREGEXP = expression(HOSTNAMECOMPONENTREGEXP,
            optional(repeated(literal("."), HOSTNAMECOMPONENTREGEXP)),
            optional(literal(":"), Pattern.compile("[0-9]+")));

    public static final Pattern TAGREGEXP = Pattern.compile("[\\w][\\w.-]{0,127}");

    public static final Pattern ANCHOREDTAGREGEXP = anchored(TAGREGEXP);

    public static final Pattern DIGESTREGEXP = Pattern
            .compile("[A-Za-z][A-Za-z0-9]*(?:[-_+.][A-Za-z][A-Za-z0-9]*)*[:][0-9A-Fa-f]{32,}");

    public static final Pattern ANCHOREDDIGESTREGEXP = anchored(DIGESTREGEXP);

    public static final Pattern NAMEREGEXP = expression(optional(HOSTNAMEREGEXP, literal("/")), NAMECOMPONENTREGEXP,
            optional(repeated(literal("/"), NAMECOMPONENTREGEXP)));

    public static final Pattern ANCHOREDNAMEREGEXP = anchored(optional(capture(HOSTNAMEREGEXP), literal("/")),
            capture(NAMECOMPONENTREGEXP, optional(repeated(literal("/"), NAMECOMPONENTREGEXP))));

    public static final Pattern REFERENCEREGEXP = anchored(capture(NAMEREGEXP),
            optional(literal(":"), capture(TAGREGEXP)), optional(literal("@"), capture(DIGESTREGEXP)));

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
