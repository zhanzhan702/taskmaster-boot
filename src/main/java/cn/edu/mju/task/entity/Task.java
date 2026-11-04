package cn.edu.mju.task.entity;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * 任务实体（JavaBean）。
 *
 * <p>实验二引入：把 {@code String[]} 的下标 0~5 换成有名字的属性，
 * 让"列的含义"写在类型里而不是程序员的记忆里。
 *
 * <p>严格遵循 JavaBean 规范：public 无参构造器 + 私有字段 + getter/setter，
 * 这样才能被 Spring MVC 的 {@code @ModelAttribute} 按名字绑定、
 * 也能被 JSP 的 EL 表达式（{@code ${task.title}}）反射读取。
 */
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String title;
    private Byte status;
    private Byte priority;

    /**
     * 截止日期。
     *
     * <p>环节四加上了 {@code @DateTimeFormat}：
     * 表单提交的是字符串 {@code "2026-12-01"}，而这里是 {@link Date}，
     * 没有这个注解 Spring MVC 无法完成转换，会直接返回 400。
     * 有了它，{@code @ModelAttribute} 就能自动把字符串解析成日期 ——
     * 这正是实验二里 {@code WebUtil.toDateOrNull()} 手工做的事，
     * 现在由框架的类型转换器接管（呼应痛点清单第 5 条）。
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date dueDate;

    private Date createDate;

    public Task() {
    }

    public Task(Integer id, String title, Byte status, Byte priority, Date dueDate, Date createDate) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.priority = priority;
        this.dueDate = dueDate;
        this.createDate = createDate;
    }

    // ==================== 只读的"显示用"属性 ====================
    // 这些 getter 存在的原因很直接：没有标签库、没有格式化器，
    // 只能自己在实体里把 0/1/2 翻成中文、把 Date 格式化成 yyyy-MM-dd。
    // 这本身就是"原生开发"的一处重复劳动（见痛点清单第 6 条）。

    public String getStatusLabel() {
        if (status == null) {
            return "未知";
        }
        switch (status.byteValue()) {
            case 0:
                return "待办";
            case 1:
                return "进行中";
            case 2:
                return "已完成";
            default:
                return "未知";
        }
    }

    public String getPriorityLabel() {
        if (priority == null) {
            return "未知";
        }
        switch (priority.byteValue()) {
            case 1:
                return "低";
            case 2:
                return "中";
            case 3:
                return "高";
            default:
                return "未知";
        }
    }

    public String getDueDateText() {
        return format(dueDate);
    }

    public String getCreateDateText() {
        return format(createDate);
    }

    /** SimpleDateFormat 非线程安全，故每次新建，不做静态复用。 */
    private static String format(Date date) {
        return date == null ? "" : new SimpleDateFormat("yyyy-MM-dd").format(date);
    }

    // ==================== 标准存取器 ====================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Byte getPriority() {
        return priority;
    }

    public void setPriority(Byte priority) {
        this.priority = priority;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @Override
    public String toString() {
        return "Task{id=" + id + ", title='" + title + "', status=" + status
                + ", priority=" + priority + ", dueDate=" + getDueDateText() + '}';
    }
}
