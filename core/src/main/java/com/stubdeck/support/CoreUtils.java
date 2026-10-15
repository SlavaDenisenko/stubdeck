package com.stubdeck.support;

import java.util.Locale;

public final class CoreUtils {
    private CoreUtils() {
    }

    public static String prepareUrl(String scheme, String host, int port) {
        return scheme + "://" + host + ":" + port;
    }

    public static boolean nullOrBlank(String string) {
        return string == null || string.isBlank();
    }

    public static String normalize(String value) {
        return nullOrBlank(value) ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
