package cn.edu.mju.task;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SpringBoot 主启动类 —— <b>实验四环节一：XML 配置全面退场的终点</b>。
 *
 * <p>与实验三对照，本类替代了原先 web.xml + 三套 XML 的四项职责：
 * <table border="1">
 *   <tr><th>实验三的做法</th><th>实验四的做法</th></tr>
 *   <tr><td>web.xml 里 &lt;context-param&gt; + ContextLoaderListener 启动 Spring 容器</td>
 *       <td>{@code SpringApplication.run()} 一行</td></tr>
 *   <tr><td>web.xml 里注册 DispatcherServlet + 映射 "/"</td>
 *       <td>{@code spring-boot-starter-web} 自动配置，无需声明</td></tr>
 *   <tr><td>applicationContext.xml 里声明 DataSource</td>
 *       <td>{@code application.yml} 的 {@code spring.datasource.*} → 自动配置</td></tr>
 *   <tr><td>applicationContext.xml 里声明 SqlSessionFactoryBean</td>
 *       <td>{@code mybatis-spring-boot-starter} 依据 yml 自动装配</td></tr>
 *   <tr><td>applicationContext.xml 里配置 MapperScannerConfigurer</td>
 *       <td>{@code @MapperScan} 一个注解</td></tr>
 *   <tr><td>applicationContext.xml 里配置 DataSourceTransactionManager + tx:annotation-driven</td>
 *       <td>{@code spring-boot-starter-jdbc}（由 mybatis starter 传递引入）自动配置事务管理器，
 *           {@code @Transactional} 直接生效</td></tr>
 *   <tr><td>spring-mvc.xml 里配置 ViewResolver</td>
 *       <td>{@code spring.mvc.view.prefix/suffix} 两个配置项</td></tr>
 *   <tr><td>spring-mvc.xml 里注册 LoginInterceptor</td>
 *       <td>{@code WebMvcConfigurer} 里用 Java 代码注册（见 WebConfig）</td></tr>
 *   <tr><td>mybatis-config.xml 里配置驼峰映射</td>
 *       <td>{@code mybatis.configuration.map-underscore-to-camel-case: true}</td></tr>
 * </table>
 *
 * <p><b>自动配置的原理</b>：{@code @SpringBootApplication} 内含
 * {@code @EnableAutoConfiguration}，它会读取各 starter 包里的
 * {@code META-INF/spring.factories}（Boot 2.7）或
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}，
 * 把一堆 {@code XxxAutoConfiguration} 类按条件（{@code @ConditionalOnClass}、
 * {@code @ConditionalOnMissingBean} 等）注册成 Bean。
 * 所以"我们没有配 DataSource，它却存在"—— 是自动配置配的。
 *
 * <p><b>{@code @MapperScan} 不能省</b>：MyBatis 的 Mapper 接口必须被扫描到，
 * 才会生成动态代理注册为 Bean。这一步 starter 不会替你做（它不知道你的包名）。
 */
@SpringBootApplication
@MapperScan("cn.edu.mju.task.mapper")
public class TaskMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskMasterApplication.class, args);
    }
}
