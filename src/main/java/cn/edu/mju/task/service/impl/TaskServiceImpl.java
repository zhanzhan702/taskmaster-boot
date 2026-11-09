package cn.edu.mju.task.service.impl;

import cn.edu.mju.task.entity.Task;
import cn.edu.mju.task.mapper.TaskMapper;
import cn.edu.mju.task.service.ServiceException;
import cn.edu.mju.task.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务业务实现 —— <b>实验五：MyBatis-Plus 版</b>。
 *
 * <h3>分页与条件查询只用了一个方法</h3>
 * <pre>
 *   LambdaQueryWrapper&lt;Task&gt; qw = Wrappers.&lt;Task&gt;lambdaQuery()
 *           .like(有标题, Task::getTitle, title)     // 动态拼条件
 *           .eq(有状态, Task::getStatus, status)
 *           .orderByAsc(Task::getId);
 *   Page&lt;Task&gt; page = new Page&lt;&gt;(pageNum, pageSize);
 *   taskMapper.selectPage(page, qw);                  // 一条语句拿到数据 + 总数
 * </pre>
 *
 * <p>对比实验三：那里要手写两条 SQL（{@code findByQuery} + {@code countByQuery}），
 * 还要额外抽一个 {@code <sql>} 片段保证两处条件一致，否则总页数会算错。
 * 现在 COUNT 由分页插件自动生成，<b>口径不一致的风险从「靠人保证」变成「结构上不可能」</b>。
 *
 * <h3>但请同时注意它的代价（报告里会展开）</h3>
 * <ul>
 *   <li><b>SQL 不可见</b>：代码里看不到 {@code LIMIT}，也看不到 {@code SELECT COUNT(*)}；</li>
 *   <li>分页插件会<b>改写</b>你写的 SQL —— 排查慢查询时看到的语句与代码对不上；</li>
 *   <li>{@code .like()} 的模糊匹配由 {@code <if>} 动态拼装，条件为空时会静默忽略，
 *       写错字段名也只有运行到才报错。</li>
 * </ul>
 */
@Service
public class TaskServiceImpl implements TaskService {

    /** 标题最大长度，与表结构 varchar(255) 对齐。 */
    private static final int TITLE_MAX_LENGTH = 255;

    /** 每页条数上限，防止 URL 传 pageSize=999999 把整表拉进内存。 */
    private static final int MAX_PAGE_SIZE = 100;

    private final TaskMapper taskMapper;

    public TaskServiceImpl(TaskMapper taskMapper) {
        if (taskMapper == null) {
            throw new IllegalArgumentException("taskMapper 不能为 null");
        }
        this.taskMapper = taskMapper;
    }

    @Override
    public IPage<Task> search(String title, Byte status, int pageNum, int pageSize) {
        // 修正分页参数：来自 URL 的值不可信
        if (pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            pageSize = 10;
        }

        String keyword = (title == null) ? null : title.trim();

        // 条件构造器：第一个布尔参数为 false 时，该条件整体不参与 SQL
        // —— 这就是「动态 SQL」在 MyBatis-Plus 里的表达方式，不必再写 <if>
        LambdaQueryWrapper<Task> qw = Wrappers.<Task>lambdaQuery()
                .like(keyword != null && !keyword.isEmpty(), Task::getTitle, keyword)
                .eq(status != null, Task::getStatus, status)
                .orderByAsc(Task::getId);

        Page<Task> page = new Page<Task>(pageNum, pageSize);
        try {
            IPage<Task> result = taskMapper.selectPage(page, qw);
            // 页码越界（例如删光了最后一页）时回退到最后一页，避免用户看到空白页
            if (result.getTotal() > 0 && pageNum > result.getPages()) {
                Page<Task> last = new Page<Task>(result.getPages(), pageSize);
                return taskMapper.selectPage(last, qw);
            }
            return result;
        } catch (DataAccessException e) {
            throw new ServiceException("查询任务列表失败", e);
        }
    }

    @Override
    public Task findById(int id) {
        try {
            return taskMapper.selectById(id);
        } catch (DataAccessException e) {
            throw new ServiceException("查询任务失败, id=" + id, e);
        }
    }

    @Override
    public void add(Task task) {
        validate(task);
        try {
            int rows = taskMapper.insert(task);
            if (rows != 1) {
                throw new ServiceException("新增任务失败，受影响行数为 " + rows);
            }
        } catch (DataAccessException e) {
            throw new ServiceException("新增任务失败", e);
        }
    }

    @Override
    public void update(Task task) {
        validate(task);
        if (task.getId() == null) {
            throw new ServiceException("更新任务时必须提供 id");
        }
        try {
            int rows = taskMapper.updateById(task);
            if (rows == 0) {
                throw new ServiceException("要修改的任务不存在, id=" + task.getId());
            }
        } catch (DataAccessException e) {
            throw new ServiceException("更新任务失败", e);
        }
    }

    @Override
    public void delete(int id) {
        try {
            int rows = taskMapper.deleteById(id);
            if (rows == 0) {
                throw new ServiceException("要删除的任务不存在, id=" + id);
            }
        } catch (DataAccessException e) {
            throw new ServiceException("删除任务失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(int id, String action) {
        try {
            doChangeStatus(id, action);
        } catch (DataAccessException e) {
            throw new ServiceException("任务状态流转失败, id=" + id + ", action=" + action, e);
        }
    }

    private void doChangeStatus(int id, String action) {
        Task task = taskMapper.selectById(id);
        if (task == null) {
            throw new ServiceException("任务不存在, id=" + id);
        }

        byte current = task.getStatus() == null ? 0 : task.getStatus().byteValue();
        byte next;
        if ("start".equals(action)) {
            if (current != 0) {
                throw new ServiceException("只有「待办」状态的任务可以开始，当前状态: "
                        + task.getStatusLabel());
            }
            next = 1;
        } else if ("complete".equals(action)) {
            if (current != 1) {
                throw new ServiceException("只有「进行中」状态的任务可以完成，当前状态: "
                        + task.getStatusLabel());
            }
            next = 2;
        } else if ("reopen".equals(action)) {
            if (current != 2) {
                throw new ServiceException("只有「已完成」状态的任务可以重新打开，当前状态: "
                        + task.getStatusLabel());
            }
            next = 1;
        } else {
            throw new ServiceException("未知的状态操作: " + action);
        }

        // 只更新 status 一个字段：用 updateById 会带上全部非空字段，
        // 因此这里构造一个只含 id + status 的临时对象，语义更精确
        Task patch = new Task();
        patch.setId(Integer.valueOf(id));
        patch.setStatus(Byte.valueOf(next));

        int rows = taskMapper.updateById(patch);
        if (rows != 1) {
            throw new ServiceException("状态更新失败，受影响行数为 " + rows);
        }
    }

    /** 业务规则校验：集中在此层，Web 层不必再管。 */
    private void validate(Task task) {
        if (task == null) {
            throw new ServiceException("任务对象不能为空");
        }
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new ServiceException("任务名称不能为空");
        }
        if (task.getTitle().trim().length() > TITLE_MAX_LENGTH) {
            throw new ServiceException("任务名称长度不能超过 " + TITLE_MAX_LENGTH + " 个字符");
        }
        if (task.getPriority() == null || task.getPriority().byteValue() < 1 || task.getPriority().byteValue() > 3) {
            throw new ServiceException("优先级取值应为 1~3");
        }
        if (task.getStatus() == null || task.getStatus().byteValue() < 0 || task.getStatus().byteValue() > 2) {
            throw new ServiceException("任务状态取值应为 0~2");
        }
        if (task.getDueDate() == null) {
            throw new ServiceException("截止日期不能为空");
        }
    }
}
