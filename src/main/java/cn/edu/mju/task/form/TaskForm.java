package cn.edu.mju.task.form;

import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * 任务表单对象（View Model）—— <b>实验五环节四：Spring Validation</b>。
 *
 * <h3>为什么单独建一个表单对象，而不是直接在实体上打注解？</h3>
 * <ul>
 *   <li>实体 {@code Task} 里还有 id、createDate 这类「不由用户填写」的字段，
 *       如果直接在实体上校验，就要额外处理"新增时 id 必须为空"之类的规则；</li>
 *   <li>表单对象的字段集合恰好等于「用户能提交的字段」，边界清晰；</li>
 *   <li>校验规则属于<b>接口契约</b>，将来若加 App 接口，各自可以有各自的表单对象。</li>
 * </ul>
 *
 * <h3>客户端校验为什么不够</h3>
 * <p>模板里会写 {@code required}、{@code maxlength}、{@code type=date}，这些只是
 * <b>浏览器给的便利</b>，可以完全绕过：按 F12 改 DOM、用 curl/Postman 直接发请求、
 * 甚至禁用 JavaScript 都能让它们失效。所以服务端必须再校验一遍，
 * 而 {@code @Valid} 就是「服务端最后一道闸」。
 *
 * <h3>校验注解在这里各自防什么</h3>
 * <table border="1">
 *   <tr><th>注解</th><th>防的问题</th></tr>
 *   <tr><td>{@code @NotBlank}</td><td>null、空串、纯空白（三者都算非法）</td></tr>
 *   <tr><td>{@code @Size(max=255)}</td><td>超长导致数据库报错或截断</td></tr>
 *   <tr><td>{@code @NotNull}</td><td>未选择（下拉框被手工删掉提交空值）</td></tr>
 *   <tr><td>{@code @Min/@Max}</td><td>越界值（如 priority=99、status=-1）</td></tr>
 * </table>
 */
public class TaskForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 修改时携带的主键；新增时为 null。 */
    private Integer id;

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 255, message = "任务名称长度不能超过 255 个字符")
    private String title;

    @NotNull(message = "优先级别不能为空")
    @Min(value = 1, message = "优先级取值应为 1~3")
    @Max(value = 3, message = "优先级取值应为 1~3")
    private Byte priority;

    @NotNull(message = "任务状态不能为空")
    @Min(value = 0, message = "任务状态取值应为 0~2")
    @Max(value = 2, message = "任务状态取值应为 0~2")
    private Byte status;

    @NotNull(message = "截止日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date dueDate;

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

    public Byte getPriority() {
        return priority;
    }

    public void setPriority(Byte priority) {
        this.priority = priority;
    }

    public Byte getStatus() {
        return status;
    }

    public void setStatus(Byte status) {
        this.status = status;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
}
