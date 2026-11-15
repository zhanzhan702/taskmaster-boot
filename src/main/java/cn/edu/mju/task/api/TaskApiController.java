package cn.edu.mju.task.api;

import cn.edu.mju.task.entity.Task;
import cn.edu.mju.task.service.TaskService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务查询 API（JSON）—— <b>实验六环节二：对外接口</b>。
 *
 * <p>为什么单独建一个 {@code @RestController}，而不是给页面控制器加 {@code @ResponseBody}？
 * <ul>
 *   <li>页面接口返回 HTML（视图名），API 返回 JSON，两者是<b>不同的契约</b>，混在一起会让
 *       每个方法都变成一堆 {@code if (isAjax)} 判断；</li>
 *   <li>页面接口可以被表单直接调用（浏览器行为），API 则需要稳定的入参校验与错误码，
 *       分开之后各自的演进互不影响。</li>
 * </ul>
 *
 * <p>本类只提供<b>读</b>接口。写接口（新增/修改/删除）仍走页面表单 + PRG，
 * 原因是：给外部开放写接口需要先解决鉴权令牌、幂等、审计等一整套问题，
 * 本项目作为教学演示，刻意不开放写接口（这也是「验收交付」时应说明的边界）。
 */
@Tag(name = "任务查询 API", description = "只读接口，供外部系统集成使用")
@RestController
@RequestMapping("/api/tasks")
public class TaskApiController {

    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 100;

    private final TaskService taskService;

    public TaskApiController(TaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "分页查询任务",
            description = "支持按任务名称模糊、按状态精确筛选。页码越界会自动回退到最后一页；"
                    + "size 超过 100 会被夹紧到 100。")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "查询成功",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "参数不合法（page 非数字、status 越界等）",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping
    public ApiResponse<ApiPage<TaskDto>> list(
            @Parameter(description = "任务名称关键字，模糊匹配；超过 100 字符会被截断", example = "报告")
            @RequestParam(name = "title", required = false) String title,

            @Parameter(description = "任务状态：0 待办 / 1 进行中 / 2 已完成；不传表示全部", example = "0")
            @RequestParam(name = "status", required = false) String status,

            @Parameter(description = "页码，从 1 开始；非法值按 1 处理", example = "1")
            @RequestParam(name = "page", required = false) String page,

            @Parameter(description = "每页条数，默认 5，上限 100", example = "5")
            @RequestParam(name = "size", required = false) String size) {

        String keyword = (title == null) ? "" : title.trim();
        if (keyword.length() > 100) {
            keyword = keyword.substring(0, 100);
        }

        Byte statusValue = parseStatus(status);
        int pageNum = parsePage(page);
        int pageSize = parseSize(size);

        IPage<Task> result = taskService.search(keyword, statusValue, pageNum, pageSize);

        ApiPage<TaskDto> data = new ApiPage<TaskDto>(
                TaskDto.from(result.getRecords()),
                result.getTotal(), result.getPages(), result.getCurrent(), result.getSize());
        return ApiResponse.ok(data);
    }

    @Operation(summary = "按编号查询任务", description = "任务不存在时返回 HTTP 404 + code=404")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "查询成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "任务不存在",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskDto>> detail(
            @Parameter(description = "任务编号", example = "1") @PathVariable("id") int id) {

        Task task = taskService.findById(id);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<TaskDto>fail(404, "任务不存在, id=" + id));
        }
        return ResponseEntity.ok(ApiResponse.ok(TaskDto.from(task)));
    }

    // ==================== 参数解析（宽松容错，与页面接口保持一致）====================

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

    private static int parseSize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return DEFAULT_SIZE;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            if (v < 1) {
                return DEFAULT_SIZE;
            }
            return Math.min(v, MAX_SIZE);
        } catch (NumberFormatException e) {
            return DEFAULT_SIZE;
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
}
