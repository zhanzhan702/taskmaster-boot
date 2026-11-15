package cn.edu.mju.task.controller;

import cn.edu.mju.task.service.ServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;

/**
 * 统一异常处理 —— <b>实验五环节四</b>。
 *
 * <h3>为什么需要它</h3>
 * 实验四里每个控制器方法都写 {@code try { ... } catch (ServiceException e) { ... }}，
 * 5 个写操作就是 5 段重复代码，且「异常 → 页面」的映射规则散落各处：
 * 有的回表单、有的重定向、有的抛 500。加一个新功能就可能忘了处理异常。
 *
 * <p>{@code @ControllerAdvice} 把这类横切逻辑集中到一处：
 * 控制器方法内部不再需要 try/catch，异常统一在这里被翻译成「HTTP 状态码 + 错误页面」。
 *
 * <h3>为什么要为 Spring 自己的异常单独写处理器</h3>
 * 一旦声明了 {@code @ExceptionHandler(Exception.class)}，它就是「兜底」，
 * 会把所有异常都吞掉 —— 包括 Spring MVC 用来表达 405 / 404 的
 * {@link HttpRequestMethodNotSupportedException} 等。
 * 若不单独处理，GET 访问只接受 POST 的接口就会从「405」变成「500」，
 * 语义就错了。所以必须<b>先具体后笼统</b>：越具体的异常排在越前面。
 *
 * <h3>关于「不泄漏技术细节」</h3>
 * 兜底的 {@code Exception} 处理器只给用户看一句通用提示，
 * 真实堆栈写进服务端日志。这是安全惯例：把内部实现暴露给攻击者没有好处。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Log log = LogFactory.getLog(GlobalExceptionHandler.class);

    /** 业务异常：属于「用户输入不合规」，返回 400。 */
    @ExceptionHandler(ServiceException.class)
    public String handleServiceException(ServiceException e, Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        model.addAttribute("status", 400);
        model.addAttribute("message", e.getMessage());
        return "error";
    }

    /** 方法不支持：只有 @PostMapping 的接口被 GET 访问时抛此异常，返回 405。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public String handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                           Model model, HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        model.addAttribute("status", 405);
        model.addAttribute("message", "该地址不支持 " + e.getMethod() + " 请求，请使用正确的提交方式");
        return "error";
    }

    /**
     * 【Code Review 修正 №3】原来这里还有一个 {@code NoHandlerFoundException} 的处理器，
     * 但它其实是<b>死代码</b>：
     * <ul>
     *   <li>Spring Boot 默认 {@code spring.mvc.throw-exception-if-no-handler-found=false}，
     *       找不到路由时<b>不会</b>抛这个异常，而是交给 {@code BasicErrorController} 返回 JSON；</li>
     *   <li>若强行打开该开关，还必须同时把 {@code spring.web.resources.add-mappings} 设为 false
     *       才能真正命中，而那会连 swagger-ui 的静态资源一起关掉。</li>
     * </ul>
     * 权衡后决定：删掉这个处理器，让 404 走 Boot 的默认处理。
     * 这样虽然 404 返回的是 JSON 而非我们的 error.html，但不会为了「一个错误页」牺牲掉静态资源映射。
     * 结论：<b>能跑的代码不等于有用的代码</b>，没被触发的分支留着只会误导后来的人。
     */

    /** 兜底：未预期的异常返回 500，堆栈只进日志、不进页面。 */
    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model, HttpServletResponse response) {
        log.error("未预期的异常", e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        model.addAttribute("status", 500);
        model.addAttribute("message", "服务器内部错误，请稍后重试或联系管理员");
        return "error";
    }
}
