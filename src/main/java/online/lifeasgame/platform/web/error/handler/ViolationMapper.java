package online.lifeasgame.platform.web.error.handler;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import online.lifeasgame.core.error.api.model.ViolationItem;

public final class ViolationMapper {
    private ViolationMapper(){}

    public static List<ViolationItem> from(ConstraintViolationException ex, List<String> maskProps) {
        var mask = maskProps == null ? Set.<String>of()
                : maskProps.stream().map(String::toLowerCase).collect(Collectors.toSet());

        return ex.getConstraintViolations().stream()
                .map(v -> {
                    var path = v.getPropertyPath() != null ? v.getPropertyPath().toString() : "";
                    var simple = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
                    var invalid = v.getInvalidValue();
                    String shown = (invalid == null) ? "null"
                            : (mask.contains(simple.toLowerCase()) ? "***" : String.valueOf(invalid));
                    return new ViolationItem(
                            path,
                            v.getMessage() != null ? v.getMessage() : "invalid",
                            shown
                    );
                })
                .toList();
    }
}
