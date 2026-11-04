package cn.edu.mju.task.interceptor;

import cn.edu.mju.task.controller.AuthController;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 登录拦截器（环节四：由原生 LoginFilter 改造而来；实验四改为 Spring Bean）。
 *
 * <p>实验三时它由 {@code spring-mvc.xml} 里的 {@code <bean>} 手工声明；
 * 实验四加 {@code @Component} 后由组件扫描创建，再在
 * {@link cn.edu.mju.task.config.WebConfig} 中注册到拦截器链。
 *
 * <p><b>拦截器（Interceptor）与过滤器（Filter）的区别</b>：
 * <table border="1">
 *   <tr><th></th><th>Filter</th><th>Interceptor</th></tr>
 *   <tr><td>所属规范</td><td>Servlet 规范</td><td>Spring MVC</td></tr>
 *   <tr><td>作用位置</td><td>Servlet 之前，连静态资源也会拦</td>
 *       <td>DispatcherServlet 之后、Controller 之前</td></tr>
 *   <tr><td>能否拿到处理器信息</td><td>不能</td>
 *       <td>能（{@code handler} 参数，可判断目标方法/注解）</td></tr>
 *   <tr><td>注册方式</td><td>web.xml 或 @WebFilter</td><td>spring-mvc.xml</td></tr>
 * </table>
 *
 * <p>本次改用拦截器的实际收益：映射范围写成 {@code /task/**}，
 * 与控制器路由在同一个配置文件里，改路由时不容易漏改；
 * 且它只作用于 Spring MVC 的请求，不会误伤静态资源。
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        HttpSession session = request.getSession(false);
        Object loginUser = (session == null) ? null : session.getAttribute(AuthController.SESSION_USER);

        if (loginUser != null) {
            return true;   // 已登录，放行
        }

        // 未登录：记住原目标并跳转到登录页（登录后跳回）
        String target = request.getRequestURI();
        String query = request.getQueryString();
        if (query != null && !query.isEmpty()) {
            target = target + "?" + query;
        }
        response.sendRedirect(request.getContextPath() + "/login?redirect="
                + java.net.URLEncoder.encode(target, "UTF-8"));
        return false;      // 中断，不再进入控制器
    }
}
