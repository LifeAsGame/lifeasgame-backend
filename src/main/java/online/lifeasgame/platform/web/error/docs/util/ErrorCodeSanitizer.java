package online.lifeasgame.platform.web.error.docs.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class ErrorCodeSanitizer {

    private static final Pattern SAFE = Pattern.compile("[A-Z0-9._-]+", Pattern.CASE_INSENSITIVE);
    private static final Pattern HYPHEN_LIKE = Pattern.compile("[\u2010\u2011\u2012\u2013\u2014\u2015\u2212\uFE58\uFE63\uFF0D]");

    private ErrorCodeSanitizer() {}

    public static String sanitize(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw, Normalizer.Form.NFKC).trim();
        return HYPHEN_LIKE.matcher(s).replaceAll("-");
    }

    public static boolean isSafe(String code) {
        return code != null && !code.isEmpty() && SAFE.matcher(code).matches();
    }
}
