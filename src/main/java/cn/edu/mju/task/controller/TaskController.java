package cn.edu.mju.task.controller;

import cn.edu.mju.task.config.TaskProperties;
import cn.edu.mju.task.entity.Task;
import cn.edu.mju.task.service.ServiceException;
import cn.edu.mju.task.service.TaskService;
import cn.edu.mju.task.util.WebUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 任务控制器 —— <b>环节四：整合 SSM，把全部 CRUD 与状态流转收进一个 @Controller</b>。
 *
 * <p>实验二时这些功能分散在 4 个 Servlet 里，每个都要在 web.xml 注册。
 * 现在一个类就装下了 7 个端点，路由写在方法注解上，新增功能不必动配置。
 *
 * <h3>路由一览</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>URL</th><th>作用</th><th>返回</th></tr>
 *   <tr><td>GET</td><td>/task/list</td><td>列表</td><td>转发到 list.jsp</td></tr>
 *   <tr><td>GET</td><td>/task/add</td><td>新增表单</td><td>转发到 add.jsp</td></tr>
 *   <tr><td>POST</td><td>/task/add</td><td>提交新增</td><td>重定向（PRG）</td></tr>
 *   <tr><td>GET</td><td>/task/edit/{id}</td><td>修改表单</td><td>转发到 edit.jsp</td></tr>
 *   <tr><td>POST</td><td>/task/edit/{id}</td><td>提交修改</td><td>重定向（PRG）</td></tr>
 *   <tr><td>POST</td><td>/task/delete/{id}</td><td>删除</td><td>重定向（PRG）</td></tr>
 *   <tr><td>POST</td><td>/task/status/{id}</td><td>状态流转</td><td>重定向（PRG）</td></tr>
 * </table>
 *
 * <h3>相比原生 Servlet 的四处改进</h3>
 * <ol>
 *   <li><b>路径变量</b>：{@code /task/edit/{id}} 取代 {@code ?id=} 与隐藏域，
 *       id 写在 URL 里，语义清晰；</li>
 *   <li><b>自动绑定</b>：{@code @ModelAttribute} 按参数名把表单装进 Task，
 *       日期靠 {@code @DateTimeFormat} 自动转换，不再手工 getParameter + parse；</li>
 *   <li><b>Flash 属性</b>：{@code RedirectAttributes.addFlashAttribute}
 *       让提示消息跨过一次重定向后自动消失，
 *       不必再拼 {@code ?msg=added}（呼应痛点清单第 9 条）；</li>
 *   <li><b>方法级路由</b>：GET/POST 由注解区分，GET 访问只接受 POST 的端点
 *       自动返回 405，不用手写 doGet 报错。</li>
 * </ol>
 */
@Controller
@RequestMapping("/task")
public class TaskController {

    private static final String VIEW_LIST = "list";
    private static final String VIEW_ADD = "add";
    private static final String VIEW_EDIT = "edit";

    private static final String ATTR_TASKS = "tasks";
    private static final String ATTR_TASK = "task";
    private static final String ATTR_MSG = "msg";
    private static final String ATTR_ERROR = "error";

    private static final String REDIRECT_LIST = "redirect:/task/list";

    /** 业务对象：控制器由 Spring 创建，因此可直接构造器注入。 */
    private final TaskService taskService;

    /** 业务参数（实验四环节二）：页面右上角显示当前环境标识。 */
    private final TaskProperties taskProperties;

    public TaskController(TaskService taskService, TaskProperties taskProperties) {
        this.taskService = taskService;
        this.taskProperties = taskProperties;
    }

    // ==================== 查询 ====================

    /**
     * 任务列表 —— <b>"查询用转发"</b>。
     *
     * <p>返回逻辑视图名，由 ViewResolver 拼成 /WEB-INF/jsp/list.jsp 并转发，
     * 地址栏保持 /task/list 不变；刷新只会重新查询，不产生副作用。
     */
    @GetMapping("/list")
    public String list(@RequestParam(name = "msg", required = false) String msg, Model model) {
        List<Task> tasks = taskService.findAll();
        model.addAttribute(ATTR_TASKS, tasks);

        // 环境标识：切 profile 后这里会变，用于肉眼确认当前连的是哪个环境
        model.addAttribute("envLabel", taskProperties.getEnvLabel());
        model.addAttribute("systemName", taskProperties.getSystemName());

        // 兼容旧的 ?msg= 直链（Flash 属性会以模型属性的形式自动出现在 model 里）
        if (msg != null && !msg.isEmpty() && !model.containsAttribute(ATTR_MSG)) {
            String text = resolveMsg(WebUtil.trim(msg));
            if (text != null) {
                model.addAttribute(ATTR_MSG, text);
            }
        }
        return VIEW_LIST;
    }

    // ==================== 新增 ====================

    /** 显示新增表单。 */
    @GetMapping("/add")
    public String addForm(Model model) {
        Task task = new Task();
        task.setTitle("");
        task.setStatus(Byte.valueOf((byte) 0));
        task.setPriority(Byte.valueOf((byte) 1));
        model.addAttribute(ATTR_TASK, task);
        return VIEW_ADD;
    }

    /**
     * 提交新增（PRG：成功后重定向，刷新不会重复插入）。
     *
     * <p>{@code @ModelAttribute} 会自动把 {@code title / status / priority / dueDate}
     * 按名字绑到 Task 上；{@link BindingResult} 必须紧跟其后，
     * 否则类型转换失败会直接抛 400 而不会进方法。
     */
    @PostMapping("/add")
    public String add(@ModelAttribute(ATTR_TASK) Task task,
                      BindingResult binding,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        String error = firstError(binding);
        if (error == null) {
            try {
                taskService.add(task);
                redirectAttributes.addFlashAttribute(ATTR_MSG, "新增成功");
                return REDIRECT_LIST;
            } catch (ServiceException e) {
                error = e.getMessage();
            }
        }
        model.addAttribute(ATTR_ERROR, error);
        return VIEW_ADD;
    }

    // ==================== 修改 ====================

    /** 显示修改表单；id 来自路径变量。 */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") int id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Task task = taskService.findById(id);
        if (task == null) {
            redirectAttributes.addFlashAttribute(ATTR_MSG, "任务不存在或已被删除");
            return REDIRECT_LIST;
        }
        model.addAttribute(ATTR_TASK, task);
        return VIEW_EDIT;
    }

    /** 提交修改。 */
    @PostMapping("/edit/{id}")
    public String edit(@PathVariable("id") int id,
                       @ModelAttribute(ATTR_TASK) Task task,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        task.setId(Integer.valueOf(id));

        String error = firstError(binding);
        if (error == null) {
            try {
                taskService.update(task);
                redirectAttributes.addFlashAttribute(ATTR_MSG, "修改成功");
                return REDIRECT_LIST;
            } catch (ServiceException e) {
                error = e.getMessage();
            }
        }
        model.addAttribute(ATTR_ERROR, error);
        return VIEW_EDIT;
    }

    // ==================== 删除 ====================

    /**
     * 删除（仅 POST）。
     *
     * <p>只写 {@code @PostMapping}，用 GET 访问会由 Spring 自动返回
     * <b>405 Method Not Allowed</b>，不必再手写 doGet 报错。
     */
    @PostMapping("/delete/{id}")
    public String delete(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
        try {
            taskService.delete(id);
            redirectAttributes.addFlashAttribute(ATTR_MSG, "删除成功");
        } catch (ServiceException e) {
            redirectAttributes.addFlashAttribute(ATTR_MSG, "任务不存在或已被删除");
        }
        return REDIRECT_LIST;
    }

    // ==================== 状态流转 ====================

    /**
     * 状态流转：{@code action} 取值 start / complete / reopen。
     *
     * <p>合法性校验（当前状态是否允许该操作）在 Service 层，且该方法带事务，
     * 保证"查状态 → 判合法 → 改状态"两步的原子性。
     */
    @PostMapping("/status/{id}")
    public String changeStatus(@PathVariable("id") int id,
                               @RequestParam("action") String action,
                               RedirectAttributes redirectAttributes) {
        try {
            taskService.changeStatus(id, action);
            redirectAttributes.addFlashAttribute(ATTR_MSG, "状态已更新");
        } catch (ServiceException e) {
            redirectAttributes.addFlashAttribute(ATTR_MSG, "状态更新失败：" + e.getMessage());
        }
        return REDIRECT_LIST;
    }

    // ==================== 辅助 ====================

    /** 把绑定错误翻译成一句可读提示。 */
    private String firstError(BindingResult binding) {
        if (binding == null || !binding.hasErrors()) {
            return null;
        }
        if (binding.hasFieldErrors("dueDate")) {
            return "截止日期格式应为 yyyy-MM-dd";
        }
        if (binding.hasFieldErrors("priority")) {
            return "优先级取值应为 1~3";
        }
        if (binding.hasFieldErrors("status")) {
            return "任务状态取值应为 0~2";
        }
        return "表单内容格式不正确，请检查后重新提交";
    }

    /** 把消息代码翻译成中文提示（兼容旧链接）。 */
    private String resolveMsg(String code) {
        if (code == null) {
            return null;
        }
        if ("added".equals(code)) {
            return "新增成功";
        }
        if ("updated".equals(code)) {
            return "修改成功";
        }
        if ("deleted".equals(code)) {
            return "删除成功";
        }
        if ("notfound".equals(code)) {
            return "任务不存在或已被删除";
        }
        return null;
    }
}
