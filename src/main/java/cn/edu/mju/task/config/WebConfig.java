package cn.edu.mju.task.config;

import cn.edu.mju.task.interceptor.LoginInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 的 Java 配置 —— <b>替代实验三的 spring-mvc.xml</b>。
 *
 * <p>实验四不再有 spring-mvc.xml。原先 XML 里的三件事分别去了三处：
 * <ul>
 *   <li>视图解析器 → {@code application.yml} 的 {@code spring.mvc.view.prefix/suffix}；</li>
 *   <li>注解驱动（{@code <mvc:annotation-driven/>}） → Boot 自动配置，无需声明；</li>
 *   <li>拦截器 → 本类（Java 代码注册，比 XML 更有类型安全，可断点调试）。</li>
 * </ul>
 *
 * <p>仍是"配置类"而不是"XML"：这是 Spring Boot 的取向 ——
 * <b>能用代码表达的配置就不写 XML</b>，因为编译期能查错、IDE 能跳转、重构能跟着改名。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor) {
        this.loginInterceptor = loginInterceptor;
    }

    /**
     * 注册登录拦截器。
     *
     * <p>只拦 {@code /task/**}：登录页 {@code /login} 必须放行，否则会无限重定向。
     *
     * <p>实验六补充：OpenAPI 文档路径必须一并放行，
     * 否则访问 {@code /swagger-ui/index.html} 会被重定向到登录页，
     * 看起来像是「springdoc 没生效」——这是接入文档工具时最常踩的坑。
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // /api/** 同样要求登录：虽然它只读，但任务标题属于业务数据，不能匿名拉取。
                // 代价是 API 调用方必须先 POST /login 拿到会话 Cookie —— 这恰恰说明
                // 「基于 Session 的认证不适合做对外 API」，正式项目应改用 Token。
                .addPathPatterns("/task/**", "/api/**")
                .excludePathPatterns("/login", "/logout",
                        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
    }
}
