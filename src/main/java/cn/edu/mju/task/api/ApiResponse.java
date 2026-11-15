package cn.edu.mju.task.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 统一 API 响应体 —— <b>实验六环节二</b>。
 *
 * <p>页面接口（{@code @Controller}）返回的是 HTML，不需要统一响应结构；
 * 但对外提供的 JSON 接口必须有稳定的「成功/失败」契约，否则调用方无法区分
 * 「业务失败」与「HTTP 失败」。本类就是这个契约。
 *
 * <h3>错误码定义</h3>
 * <table border="1">
 *   <tr><th>code</th><th>含义</th><th>对应 HTTP</th></tr>
 *   <tr><td>0</td><td>成功</td><td>200</td></tr>
 *   <tr><td>400</td><td>参数不合法（如 page=abc、status 越界、id 非法）</td><td>400</td></tr>
 *   <tr><td>404</td><td>资源不存在（任务 id 查不到）</td><td>404</td></tr>
 *   <tr><td>500</td><td>服务端内部错误（数据库不可用等）</td><td>500</td></tr>
 * </table>
 */
@Schema(description = "统一响应体")
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "错误码：0 表示成功", example = "0")
    private int code;

    @Schema(description = "提示信息", example = "ok")
    private String message;

    @Schema(description = "业务数据，失败时为 null")
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<T>(0, "ok", data);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<T>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
