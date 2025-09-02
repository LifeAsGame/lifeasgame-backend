package online.lifeasgame.platform.web.response;

import java.net.URI;
import online.lifeasgame.core.response.ApiResponse;
import online.lifeasgame.core.response.code.CommonSuccess;
import org.springframework.http.ResponseEntity;

public final class ApiResponses {
    private ApiResponses() {}

    public static <T> ResponseEntity<ApiResponse<T>> ok(T result) {
        return ResponseEntity.status(CommonSuccess.OK.status())
                .body(ApiResponse.onSuccess(result));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(URI location, T result) {
        return ResponseEntity.created(location)
                .body(ApiResponse.onCreateSuccess(result));
    }

    public static <T> ResponseEntity<ApiResponse<T>> deleted(T result) {
        return ResponseEntity.status(CommonSuccess.DELETED.status())
                .body(ApiResponse.onDeleteSuccess(result));
    }
}
