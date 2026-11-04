package cn.edu.mju.task;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * 外部 Tomcat 部署入口。
 *
 * <p>本项目打的是 war 包，有两种运行方式：
 * <ol>
 *   <li><b>可执行 war</b>：{@code java -jar target/taskmaster-boot.war} —— 走内嵌 Tomcat，
 *       入口是 {@link TaskMasterApplication#main}；</li>
 *   <li><b>外部 Tomcat 9</b>：把 war 丢进 {@code webapps/} —— 这时 {@code main} 不会被调用，
 *       容器通过 Servlet 规范的 SPI 找到本类，再由它把配置交回 Spring Boot。
 *       没有这个类，外部 Tomcat 部署会启动失败（找不到 Spring 容器）。</li>
 * </ol>
 *
 * <p>注意：Spring Boot <b>不支持可执行 jar 里跑 JSP</b>（Tomcat 对 jar 内 JSP 有硬编码路径限制），
 * 所以本项目坚持 war 打包。
 */
public class ServletInitializer extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TaskMasterApplication.class);
    }
}
