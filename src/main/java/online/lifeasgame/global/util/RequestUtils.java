package online.lifeasgame.global.util;

import jakarta.servlet.RequestDispatcher;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

public final class RequestUtils {
    private RequestUtils(){}

    public static String path(WebRequest req) {
        if (req instanceof ServletWebRequest swr) {
            var r = swr.getRequest();
            Object orig = r.getAttribute(RequestDispatcher.FORWARD_REQUEST_URI);
            return orig != null ? orig.toString() : r.getRequestURI();
        }
        return null;
    }

    public static String origin(WebRequest req) {
        if (req instanceof ServletWebRequest swr) {
            var r = swr.getRequest();
            boolean isDefault = ("http".equals(r.getScheme()) && r.getServerPort()==80)
                    || ("https".equals(r.getScheme()) && r.getServerPort()==443);
            return r.getScheme()+"://"+r.getServerName() + (isDefault? "" : ":"+r.getServerPort());
        }
        return "";
    }
}
