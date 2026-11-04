package cn.edu.mju.task.controller;

import cn.edu.mju.task.config.TaskProperties;
import cn.edu.mju.task.entity.User;
import cn.edu.mju.task.service.UserService;
import cn.edu.mju.task.util.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

/**
 * 登录 / 登出控制器（环节四：由原生 LoginServlet 改造而来）。
 *
 * <p>改造点：
 * <ul>
 *   <li>{@code extends HttpServlet} → {@code @Controller}；</li>
 *   <li>在 {@code init()} 里 {@code getBean} → 构造器注入 {@code UserService}；</li>
 *   <li>{@code request.getRequestDispatcher(...).forward(...)} → 返回视图名 {@code "login"}；</li>
 *   <li>错误提示改用 {@link Model}，不再手工 {@code setAttribute}。</li>
 * </ul>
 *
 * <p>注意登录页已移到 {@code /WEB-INF/jsp/login.jsp}，浏览器不能直接访问，
 * 必须经本控制器转发 —— 这样做的好处是：登录成功的跳转逻辑只有一处，
 * 不会被绕过。
 */
@Controller
public class AuthController {

    /** 会话中保存登录用户的键名（与拦截器约定一致）。 */
    public static final String SESSION_USER = "loginUser";

    private static final String VIEW_LOGIN = "login";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_LIST = "redirect:/task/list";

    private final UserService userService;

    /** 业务参数（实验四环节二）：登录页显示的环境标识与欢迎语来自 application-*.yml。 */
    private final TaskProperties taskProperties;

    public AuthController(UserService userService, TaskProperties taskProperties) {
        this.userService = userService;
        this.taskProperties = taskProperties;
    }

    /** 显示登录页。 */
    @GetMapping("/login")
    public String loginForm(@RequestParam(name = "msg", required = false) String msg, Model model) {
        if ("logout".equals(msg)) {
            model.addAttribute("notice", "您已安全退出。");
        }
        // 这两个属性来自配置，用于肉眼确认当前 profile
        model.addAttribute("systemName", taskProperties.getSystemName());
        model.addAttribute("envLabel", taskProperties.getEnvLabel());
        model.addAttribute("welcomeMessage", taskProperties.getWelcomeMessage());
        return VIEW_LOGIN;
    }

    /** 提交登录（PRG：成功后重定向）。 */
    @PostMapping("/login")
    public String login(@RequestParam("loginName") String loginName,
                        @RequestParam("loginPwd") String loginPwd,
                        @RequestParam(name = "redirect", required = false) String redirect,
                        HttpSession session,
                        Model model) {

        String name = WebUtil.trim(loginName);
        String pwd = WebUtil.trim(loginPwd);

        if (name == null || name.isEmpty() || pwd == null || pwd.isEmpty()) {
            model.addAttribute("error", "用户名和密码都不能为空");
            model.addAttribute("loginName", name);
            return VIEW_LOGIN;
        }

        User user = userService.login(name, pwd);
        if (user == null) {
            model.addAttribute("error", "用户名或密码错误");
            model.addAttribute("loginName", name);
            return VIEW_LOGIN;
        }

        session.setAttribute(SESSION_USER, user);
        // 只接受站内相对路径，避免开放重定向漏洞
        if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
            return "redirect:" + redirect;
        }
        return REDIRECT_LIST;
    }

    /** 登出：销毁会话后回到登录页。 */
    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addAttribute("msg", "logout");
        return REDIRECT_LOGIN;
    }
}
