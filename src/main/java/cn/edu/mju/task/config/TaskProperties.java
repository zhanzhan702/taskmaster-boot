package cn.edu.mju.task.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 业务参数配置（{@code application.yml} 中以 {@code task.} 为前缀的项）。
 *
 * <p><b>环节二要求的"业务参数一键隔离"就落在这里</b>：
 * 同一份代码，靠 {@code --spring.profiles.active=dev/test/prod}
 * 切换成不同的系统名、环境标识与欢迎语，无需改一行 Java。
 *
 * <p>{@code @ConfigurationProperties} 相比逐个 {@code @Value} 的优势：
 * <ul>
 *   <li>类型安全：yml 写错类型启动即报错，而不是运行到那行才炸；</li>
 *   <li>可校验：配合 {@code @Validated} + {@code @NotBlank} 能做启动期校验；</li>
 *   <li>IDE 支持：需要 {@code spring-boot-configuration-processor} 才有自动补全，
 *       但至少键名是集中声明的，不会散落各处。</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "task")
public class TaskProperties {

    /** 系统显示名，各环境可不同。 */
    private String systemName = "TaskMaster";

    /** 环境标识，显示在页面右上角，用于肉眼确认当前跑的是哪个环境。 */
    private String envLabel = "unknown";

    /** 登录页欢迎语，各环境可不同。 */
    private String welcomeMessage = "";

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getEnvLabel() {
        return envLabel;
    }

    public void setEnvLabel(String envLabel) {
        this.envLabel = envLabel;
    }

    public String getWelcomeMessage() {
        return welcomeMessage;
    }

    public void setWelcomeMessage(String welcomeMessage) {
        this.welcomeMessage = welcomeMessage;
    }
}
