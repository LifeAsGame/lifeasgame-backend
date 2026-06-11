package online.lifeasgame.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import online.lifeasgame.core.response.code.CommonSuccess;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
@ToString
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;
    private final String code;
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    public static <T> ApiResponse<T> onSuccess(T result){
        return new ApiResponse<>(true, CommonSuccess.OK.code(), CommonSuccess.OK.message(), result);
    }

    public static <T> ApiResponse<T> onCreateSuccess(T result){
        return new ApiResponse<>(true, CommonSuccess.CREATED.code(), CommonSuccess.CREATED.message(), result);
    }

    public static <T> ApiResponse<T> onDeleteSuccess(T result){
        return new ApiResponse<>(true, CommonSuccess.DELETED.code(), CommonSuccess.DELETED.message(), result);
    }

    public static <T> ApiResponse<T> onFailure(String code, String message, T data){
        return new ApiResponse<>(false, code, message, data);
    }
}
