package cn.edu.mju.task.api;

import cn.edu.mju.task.service.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * API 专用异常处理 —— <b>必须与页面用的 {@code @ControllerAdvice} 分开</b>。
 *
 * <p>页面处理器返回的是<b>视图名</b>（{@code "error"}），若它接管了 API 请求，
 * 调用方会收到一段 HTML 而不是 JSON，前端的 {@code response.json()} 直接抛解析错误。
 *
 * <p>{@code @RestControllerAdvice(basePackages = "cn.edu.mju.task.api")} 把作用范围限定在
 * API 包内，并且它的优先级高于全局的 {@code @ControllerAdvice}（Spring 按 @Order 与精确度排序，
 * 限定 basePackages 的 advice 优先匹配）。
 *
 * <p>这就是「同一个应用里有两套对外契约」时必须付出的结构成本：
 * 一个异常处理器不够用，要按契约类型分开。
 */
@RestControllerAdvice(basePackages = "cn.edu.mju.task.api")
public class ApiExceptionHandler {

    private static final Log log = LogFactory.getLog(ApiExceptionHandler.class);

    /** 业务异常 → 400 + JSON 错误码。 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleService(ServiceException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>fail(400, e.getMessage()));
    }

    /** 兜底 → 500 + JSON，堆栈只进日志。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleOther(Exception e) {
        log.error("API 未预期异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.<Void>fail(500, "服务器内部错误"));
    }
}
