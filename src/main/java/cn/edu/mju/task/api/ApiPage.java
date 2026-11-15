package cn.edu.mju.task.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 分页数据的对外结构。
 *
 * <p>不直接把 MyBatis-Plus 的 {@code IPage} 序列化出去，原因有两个：
 * <ul>
 *   <li>{@code IPage} 会带出 {@code orders}、{@code optimizeCountSql} 等内部字段，属于实现细节；</li>
 *   <li>对外契约应保持稳定，框架升级不应改变 API 结构。</li>
 * </ul>
 */
@Schema(description = "分页数据")
public class ApiPage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "总记录数", example = "12")
    private long total;

    @Schema(description = "总页数", example = "3")
    private long pages;

    @Schema(description = "当前页号，从 1 开始", example = "1")
    private long current;

    @Schema(description = "每页条数", example = "5")
    private long size;

    public ApiPage() {
    }

    public ApiPage(List<T> records, long total, long pages, long current, long size) {
        this.records = records;
        this.total = total;
        this.pages = pages;
        this.current = current;
        this.size = size;
    }

    public List<T> getRecords() {
        return records;
    }

    public long getTotal() {
        return total;
    }

    public long getPages() {
        return pages;
    }

    public long getCurrent() {
        return current;
    }

    public long getSize() {
        return size;
    }
}
