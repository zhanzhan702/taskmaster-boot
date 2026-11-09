package cn.edu.mju.task.service;

import cn.edu.mju.task.entity.Task;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 任务业务接口。
 *
 * <p>实验五的改动：
 * <ul>
 *   <li>{@code findAll()} → {@link #search(String, Byte, int, int)}：
 *       支持<b>任务名称模糊 + 状态精确</b>的条件查询，并返回分页结果；</li>
 *   <li>返回值用 MyBatis-Plus 的 {@link IPage}，它同时携带「当前页数据」与
 *       「总数、总页数、当前页号」—— 模板里直接 {@code page.records} / {@code page.total} 即可。</li>
 * </ul>
 */
public interface TaskService {

    /**
     * 条件查询 + 分页。
     *
     * @param title    任务名称关键字（模糊匹配，空则不参与筛选）
     * @param status   任务状态（精确匹配，null 表示全部）
     * @param pageNum  页码，从 1 开始；越界会被修正
     * @param pageSize 每页条数
     * @return 分页结果，永不为 null
     */
    IPage<Task> search(String title, Byte status, int pageNum, int pageSize);

    /** 按 id 查询；不存在返回 null。 */
    Task findById(int id);

    /** 新增任务。 */
    void add(Task task);

    /** 更新任务。 */
    void update(Task task);

    /** 删除任务。 */
    void delete(int id);

    /**
     * 任务状态流转。
     *
     * <p>规则：start(待办0→进行中1)、complete(进行中1→已完成2)、reopen(已完成2→进行中1)。
     *
     * <p><b>为什么必须加事务</b>：本方法要「先查当前状态 → 校验是否允许 → 再更新」，
     * 两步之间若并发改了状态，校验就会失效。加 {@code @Transactional} 后
     * 查询与更新处于同一事务，任一步异常都整体回滚。
     *
     * @param id     任务 id
     * @param action start / complete / reopen
     * @throws ServiceException 任务不存在，或当前状态不允许该操作
     */
    void changeStatus(int id, String action);
}
