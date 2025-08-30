package online.lifeasgame.platform.web.error.handler;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import online.lifeasgame.core.error.api.model.FieldErrorItem;
import org.springframework.validation.BindingResult;

public final class FieldErrorMapper {
    private FieldErrorMapper(){}

    public static List<FieldErrorItem> from(BindingResult br, List<String> maskFields) {
        var mask = maskFields == null ? Set.<String>of()
                : maskFields.stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());

        return br.getFieldErrors().stream()
                .map(
                        fe -> new FieldErrorItem(
                                fe.getField(),
                                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                                maskIfNeeded(fe.getField(), fe.getRejectedValue(), mask)
                        )
                )
                .toList();
    }

    private static String maskIfNeeded(String field, Object val, Set<String> mask) {
        if (val == null) {
            return "null";
        }

        var simple = field.contains(".") ? field.substring(field.lastIndexOf('.') + 1) : field;

        return mask.contains(simple.toLowerCase()) ? "***" : String.valueOf(val);
    }
}
