package cn.edu.mju.task.api;

import cn.edu.mju.task.entity.Task;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 对外输出的任务视图对象。
 *
 * <p>为什么不直接把 {@link Task} 实体丢给 JSON？
 * <ul>
 *   <li>实体里的 {@code Date} 会被序列化成毫秒时间戳，前端不好用；这里转成 {@code yyyy-MM-dd} 字符串；</li>
 *   <li>实体字段与「对外契约」耦合 —— 将来加内部字段（如逻辑删除标记）会自动泄漏到 API；</li>
 *   <li>输出对象可以单独演进，不影响持久层。</li>
 * </ul>
 */
@Schema(description = "任务")
public class TaskDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "任务编号", example = "1")
    private Integer id;

    @Schema(description = "任务名称", example = "完成实验报告")
    private String title;

    /**
     * 状态码。
     *
     * <p>注意 {@code type = "integer"} 不能省：OpenAPI 对 Java 的 {@code Byte}
     * 默认映射成 {@code type: string, format: byte}（即 base64 编码），
     * 生成的文档会把示例写成 {@code MA==} 这种让人看不懂的东西。
     * 显式声明成 integer 才能得到 {@code 0}。
     */
    @Schema(description = "状态码：0 待办 / 1 进行中 / 2 已完成", type = "integer", example = "0")
    private Byte status;

    @Schema(description = "状态中文名", example = "待办")
    private String statusLabel;

    @Schema(description = "优先级：1 低 / 2 中 / 3 高", type = "integer", example = "3")
    private Byte priority;

    @Schema(description = "优先级中文名", example = "高")
    private String priorityLabel;

    @Schema(description = "截止日期 yyyy-MM-dd", example = "2026-12-01")
    private String dueDate;

    @Schema(description = "创建日期 yyyy-MM-dd", example = "2026-11-09")
    private String createDate;

    public static TaskDto from(Task task) {
        if (task == null) {
            return null;
        }
        TaskDto dto = new TaskDto();
        dto.id = task.getId();
        dto.title = task.getTitle();
        dto.status = task.getStatus();
        dto.statusLabel = task.getStatusLabel();
        dto.priority = task.getPriority();
        dto.priorityLabel = task.getPriorityLabel();
        dto.dueDate = task.getDueDateText();
        dto.createDate = task.getCreateDateText();
        return dto;
    }

    public static List<TaskDto> from(List<Task> tasks) {
        List<TaskDto> list = new ArrayList<TaskDto>();
        if (tasks != null) {
            for (Task t : tasks) {
                list.add(from(t));
            }
        }
        return list;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Byte getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public Byte getPriority() {
        return priority;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public String getDueDate() {
        return dueDate;
    }

    public String getCreateDate() {
        return createDate;
    }
}
