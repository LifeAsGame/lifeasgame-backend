package online.lifeasgame.platform.web.error;

import java.util.regex.Pattern;
import online.lifeasgame.core.error.Sensitivity;
import org.springframework.stereotype.Component;

@Component
public class PiiScrubber {

    private static final Pattern JWT  = Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern CARD = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])[^@\\s]*(@)[^\\s]{1,}");

    public String scrub(String text, Sensitivity sen) {
        if (text == null || text.isBlank()) return text;
        String s = text;

        switch (sen) {
            case SECRET -> { return "**secret**"; }
            case PCI    -> s = maskCard(s);
            case PII    -> s = maskBasicPii(s);
            case NONE   -> { /* no-op */ }
        }

        s = maskJwt(s);
        s = maskCard(s);
        return s;
    }

    private String maskJwt(String s)  { return JWT.matcher(s).replaceAll("**jwt**"); }
    private String maskCard(String s) { return CARD.matcher(s).replaceAll("****-****-****-****"); }

    private String maskBasicPii(String s) {
        return EMAIL.matcher(s).replaceAll(mr -> {
            String first = mr.group(1);
            return first + "***@" + "***";
        });
    }
}
