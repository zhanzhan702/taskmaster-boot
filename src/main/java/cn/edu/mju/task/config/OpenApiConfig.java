package cn.edu.mju.task.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档元信息 —— <b>实验六环节二</b>。
 *
 * <p>springdoc 会扫描全部 {@code @Controller}，按 {@code @RequestMapping} 生成路径、
 * 参数、响应模型；本类只补「文档本身的元数据」（标题、版本、联系人）。
 *
 * <p>产出物：
 * <ul>
 *   <li>{@code GET /v3/api-docs} —— OpenAPI 3.0 JSON（本项目的 docs/API.md 由它导出）；</li>
 *   <li>{@code GET /swagger-ui/index.html} —— 可交互调试的页面。</li>
 * </ul>
 *
 * <p>注意：{@code /v3/api-docs} 与 {@code /swagger-ui/**} 必须放行，
 * 否则会被 LoginInterceptor 拦到登录页 —— 这是接入 springdoc 时最容易漏的一步
 * （见 WebConfig 的 excludePathPatterns）。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI taskMasterOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("TaskMaster 任务管理系统 API")
                .version("1.1.0")
                .description("TaskMaster 从原生 Servlet 演进到 SpringBoot + Thymeleaf + MyBatis-Plus 的"
                        + "完整接口集合。返回值为 HTML 视图（服务端渲染），"
                        + "因此接口的「出参」以页面字段与 HTTP 状态码为主。")
                .contact(new Contact().name("傅展鹏").email("2837420590@qq.com"))
                .license(new License().name("教学演示用")));
    }
}
