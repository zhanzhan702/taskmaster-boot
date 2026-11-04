package cn.edu.mju.task.service;

import cn.edu.mju.task.entity.Task;

import java.util.List;

/**
 * 任务业务接口。
 *
 * <p><b>环节一引入业务层的意义</b>：
 * 实验二里 Servlet 直接调 DAO，业务规则（标题非空、状态取值等）散落在 Servlet 与工具类中。
 * 现在把"业务规则"集中到这一层，Servlet 只负责接请求与选页面。
 *
 * <p>注意方法签名<b>不再</b> {@code throws SQLException} —— 底层技术异常已在实现里
 * 翻译成 {@link ServiceException}，上层不必感知持久化细节。
 */
public interface TaskService {

    /** 查询全部任务。 */
    List<Task> findAll();

    /** 按 id 查询；不存在返回 null。 */
    Task findById(int id);

    /** 新增任务。 */
    void add(Task task);

    /** 更新任务。 */
    void update(Task task);

    /** 删除任务。 */
    void delete(int id);

    /**
     * 任务状态流转（环节四新增）。
     *
     * <p>规则：
     * <table border="1">
     *   <tr><th>action</th><th>允许的当前状态</th><th>流转结果</th></tr>
     *   <tr><td>start</td><td>待办(0)</td><td>进行中(1)</td></tr>
     *   <tr><td>complete</td><td>进行中(1)</td><td>已完成(2)</td></tr>
     *   <tr><td>reopen</td><td>已完成(2)</td><td>进行中(1)</td></tr>
     * </table>
     *
     * <p><b>为什么必须加事务</b>：本方法要"先查当前状态 → 校验是否允许 →
     * 再更新"，两步之间如果并发有别人改了状态，校验就会失效
     * （例如两个请求同时把"待办"改成"进行中"）。
     * 加了 {@code @Transactional} 后，查询与更新在同一事务内，
     * 配合数据库的隔离级别可以避免这类竞态；
     * 同时任何一步异常都会整体回滚，不会出现"状态改了一半"。
     *
     * @param id     任务 id
     * @param action start / complete / reopen
     * @throws ServiceException 任务不存在，或当前状态不允许该操作
     */
    void changeStatus(int id, String action);
}
