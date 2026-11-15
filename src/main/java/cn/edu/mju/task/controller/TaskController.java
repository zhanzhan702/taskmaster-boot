package cn.edu.mju.task.controller;

import cn.edu.mju.task.config.TaskProperties;
import cn.edu.mju.task.entity.Task;
import cn.edu.mju.task.form.TaskForm;
import cn.edu.mju.task.service.ServiceException;
import cn.edu.mju.task.service.TaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
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

import javax.validation.Valid;

/**
 * 任务控制器 —— <b>实验五：Thymeleaf 视图 + MyBatis-Plus 分页 + @Valid 校验</b>。
 *
 * <h3>路由一览</h3>
 * <table border="1">
 *   <tr><th>方法</th><th>URL</th><th>作用</th><th>返回</th></tr>
 *   <tr><td>GET</td><td>/task/list</td><td>条件查询 + 分页列表</td><td>list.html</td></tr>
 *   <tr><td>GET</td><td>/task/add</td><td>新增表单</td><td>add.html</td></tr>
 *   <tr><td>POST</td><td>/task/add</td><td>提交新增（@Valid）</td><td>重定向 / 回表单</td></tr>
 *   <tr><td>GET</td><td>/task/edit/{id}</td><td>修改表单</td><td>edit.html</td></tr>
 *   <tr><td>POST</td><td>/task/edit/{id}</td><td>提交修改（@Valid）</td><td>重定向 / 回表单</td></tr>
 *   <tr><td>POST</td><td>/task/delete/{id}</td><td>删除</td><td>重定向</td></tr>
 *   <tr><td>POST</td><td>/task/status/{id}</td><td>状态流转</td><td>重定向</td></tr>
 * </table>
 *
 * <h3>三个与实验四不同的点</h3>
 * <ol>
 *   <li><b>不再用 {@code @ModelAttribute Task}</b>，改用表单对象 {@link TaskForm}。
 *       实体上有 id/createDate 这类用户不该提交的字段，用表单对象可以精确控制可绑定字段，
 *       配合 {@code @Valid} 才能做到「只校验用户真正提交的内容」。</li>
 *   <li><b>校验失败时不再手工拼错误字符串</b>：{@code BindingResult} 会带着每个字段的错误，
 *       模板用 {@code th:errors} 逐字段展示，错误提示就显示在对应输入框下方。</li>
 *   <li><b>分页参数由 Service 修正</b>，控制器只负责把 {@code IPage} 放进模型。</li>
 * </ol>
 */
@Controller
@RequestMapping("/task")
public class TaskController {

    private static final String VIEW_LIST = "list";
    private static final String VIEW_ADD = "add";
    private static final String VIEW_EDIT = "edit";

    private static final String ATTR_PAGE = "page";
    private static final String ATTR_FORM = "form";
    private static final String ATTR_MSG = "msg";
    private static final String ATTR_TITLE = "queryTitle";
    private static final String ATTR_STATUS = "queryStatus";

    private static final String REDIRECT_LIST = "redirect:/task/list";

    /** 每页条数（演示用，固定 5 条，便于看到翻页效果）。 */
    private static final int PAGE_SIZE = 5;

    /**
     * 查询关键字的最大长度。
     *
     * <p>【Code Review 修正 №1】原实现对 {@code title} 不限长，
     * 而它最终会变成 {@code LIKE '%关键字%'}：关键字过长既走不了索引（前置通配符本就无法用索引），
     * 又会把超长参数塞进 SQL 与日志里。这里截断到 100 字符
     * —— 因为是「筛选条件」而非「要落库的数据」，静默截断比抛 400 体验更好。
     */
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final TaskService taskService;
    private final TaskProperties taskProperties;

    public TaskController(TaskService taskService, TaskProperties taskProperties) {
        this.taskService = taskService;
        this.taskProperties = taskProperties;
    }

    // ==================== 查询 + 分页 ====================

    /**
     * 条件查询 + 分页。
     *
     * <p>参数一律用 {@code String} 接收再宽松解析：{@code page=abc} 或 {@code status=99}
     * 这类手改 URL 的情况不应让用户撞上 400 错误页，而应回退到「第 1 页 / 全部状态」。
     */
    @GetMapping("/list")
    public String list(@RequestParam(name = "title", required = false) String title,
                       @RequestParam(name = "status", required = false) String status,
                       @RequestParam(name = "page", required = false) String page,
                       Model model) {

        String keyword = (title == null) ? "" : title.trim();
        if (keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        Byte statusValue = parseStatus(status);
        int pageNum = parsePage(page);

        IPage<Task> result = taskService.search(keyword, statusValue, pageNum, PAGE_SIZE);

        // 页码窗口在 Java 里算好再交给模板。
        // 不要在 Thymeleaf 里写 T(java.lang.Math).max(1, page.current - 2)：
        // page.current 是 Long，而字面量 1 是 Integer，SpEL 解析重载时会抛
        // 「Method call of 'max' is ambiguous」，且报错发生在渲染阶段、页面只看到 500。
        int pages = (int) result.getPages();
        int current = (int) result.getCurrent();
        model.addAttribute("pageFrom", Math.max(1, current - 2));
        model.addAttribute("pageTo", Math.min(Math.max(pages, 1), current + 2));

        model.addAttribute(ATTR_PAGE, result);
        model.addAttribute(ATTR_TITLE, keyword);
        model.addAttribute(ATTR_STATUS, statusValue);
        model.addAttribute("envLabel", taskProperties.getEnvLabel());
        model.addAttribute("systemName", taskProperties.getSystemName());
        model.addAttribute("pageSize", PAGE_SIZE);
        return VIEW_LIST;
    }

    private static int parsePage(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 1;
        }
        try {
            return Math.max(Integer.parseInt(raw.trim()), 1);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static Byte parseStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        try {
            byte v = Byte.parseByte(raw.trim());
            return (v >= 0 && v <= 2) ? Byte.valueOf(v) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 新增 ====================

    /** 显示新增表单。 */
    @GetMapping("/add")
    public String addForm(Model model) {
        TaskForm form = new TaskForm();
        form.setStatus(Byte.valueOf((byte) 0));
        form.setPriority(Byte.valueOf((byte) 1));
        model.addAttribute(ATTR_FORM, form);
        return VIEW_ADD;
    }

    /**
     * 提交新增。
     *
     * <p>{@code @Valid} 触发 TaskForm 上的 JSR-303 校验，结果写入紧随其后的
     * {@link BindingResult}。注意 BindingResult <b>必须紧跟</b>在被校验对象之后，
     * 否则 Spring 会认为没有接收错误、直接抛 400。
     *
     * <p>校验通过后再由 Service 做业务校验（例如「标题恰好 255 字」这类与表结构相关的规则），
     * 两层校验的分工是：<b>@Valid 管「格式与必填」，Service 管「业务规则」</b>。
     */
    @PostMapping("/add")
    public String add(@Valid @ModelAttribute(ATTR_FORM) TaskForm form,
                      BindingResult binding,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        if (binding.hasErrors()) {
            // 模板用 th:errors 逐字段回显错误，这里不需要拼任何提示文本
            return VIEW_ADD;
        }

        try {
            taskService.add(toTask(form));
            redirectAttributes.addFlashAttribute(ATTR_MSG, "新增成功");
            return REDIRECT_LIST;
        } catch (ServiceException e) {
            // 业务校验失败：把提示挂到整个表单上（模板里的 formError 区域）
            model.addAttribute("formError", e.getMessage());
            return VIEW_ADD;
        }
    }

    // ==================== 修改 ====================

    /** 显示修改表单。 */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") int id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Task task = taskService.findById(id);
        if (task == null) {
            redirectAttributes.addFlashAttribute(ATTR_MSG, "任务不存在或已被删除");
            return REDIRECT_LIST;
        }
        model.addAttribute(ATTR_FORM, toForm(task));
        return VIEW_EDIT;
    }

    /** 提交修改。 */
    @PostMapping("/edit/{id}")
    public String edit(@PathVariable("id") int id,
                       @Valid @ModelAttribute(ATTR_FORM) TaskForm form,
                       BindingResult binding,
                       Model model,
                       RedirectAttributes redirectAttributes) {

        form.setId(Integer.valueOf(id));
        if (binding.hasErrors()) {
            return VIEW_EDIT;
        }

        try {
            Task task = toTask(form);
            task.setId(Integer.valueOf(id));
            taskService.update(task);
            redirectAttributes.addFlashAttribute(ATTR_MSG, "修改成功");
            return REDIRECT_LIST;
        } catch (ServiceException e) {
            model.addAttribute("formError", e.getMessage());
            return VIEW_EDIT;
        }
    }

    // ==================== 删除 ====================

    /** 删除（仅 POST；用 GET 访问会由 Spring 返回 405）。 */
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

    /** 状态流转：action 取值 start / complete / reopen。 */
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

    private static Task toTask(TaskForm form) {
        Task task = new Task();
        task.setTitle(form.getTitle() == null ? null : form.getTitle().trim());
        task.setStatus(form.getStatus());
        task.setPriority(form.getPriority());
        task.setDueDate(form.getDueDate());
        return task;
    }

    private static TaskForm toForm(Task task) {
        TaskForm form = new TaskForm();
        form.setId(task.getId());
        form.setTitle(task.getTitle());
        form.setStatus(task.getStatus());
        form.setPriority(task.getPriority());
        form.setDueDate(task.getDueDate());
        return form;
    }
}
