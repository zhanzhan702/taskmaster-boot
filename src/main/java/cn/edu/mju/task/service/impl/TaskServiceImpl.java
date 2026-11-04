package cn.edu.mju.task.service.impl;

import cn.edu.mju.task.entity.Task;
import cn.edu.mju.task.mapper.TaskMapper;
import cn.edu.mju.task.service.ServiceException;
import cn.edu.mju.task.service.TaskService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务业务实现 —— <b>环节三：数据访问由 JDBC DAO 换成 MyBatis Mapper</b>。
 *
 * <p>本类仍是<b>构造器注入</b>示范，只是注入的东西变了：
 * <pre>
 *   环节一：public TaskServiceImpl(TaskDao taskDao)        ← 自己写的 JDBC DAO
 *   环节三：public TaskServiceImpl(TaskMapper taskMapper)  ← MyBatis 动态代理
 * </pre>
 * 值得注意：<b>业务逻辑一行都没改</b>，改的只是依赖的类型。
 * 这正是"面向接口 + 依赖注入"的价值 —— 换持久化技术不影响业务层。
 *
 * <p>异常也变干净了：{@code TaskMapper} 不抛 {@code SQLException}
 * （MyBatis-Spring 会把底层异常翻译成 Spring 的 {@link DataAccessException}），
 * 所以这里只需捕获运行时异常，不必再写受检异常处理 —— 呼应痛点清单第 7 条。
 */
@Service
public class TaskServiceImpl implements TaskService {

    /** 构造器注入 → final，创建后不可变。 */
    private final TaskMapper taskMapper;

    /**
     * 构造器注入（实验四：改由注解驱动）。
     *
     * <p>实验三时这个 Bean 是在 {@code applicationContext.xml} 里用
     * {@code <constructor-arg ref="taskMapper"/>} 声明的；
     * 迁移到 SpringBoot 后 XML 没了，改为：
     * <ul>
     *   <li>{@code @Service} 让组件扫描发现本类；</li>
     *   <li>类上只有<b>一个</b>构造器，Spring 4.3+ 会<b>隐式</b>按类型注入其参数，
     *       连 {@code @Autowired} 都可以省略。</li>
     * </ul>
     * 依赖仍是 {@code final} —— 构造器注入的性质没有因为换配置方式而改变。
     */
    public TaskServiceImpl(TaskMapper taskMapper) {
        if (taskMapper == null) {
            throw new IllegalArgumentException("taskMapper 不能为 null");
        }
        this.taskMapper = taskMapper;
    }

    @Override
    public List<Task> findAll() {
        try {
            return taskMapper.findAll();
        } catch (DataAccessException e) {
            throw new ServiceException("查询任务列表失败", e);
        }
    }

    @Override
    public Task findById(int id) {
        try {
            return taskMapper.findById(id);
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
            int rows = taskMapper.update(task);
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

    /**
     * 任务状态流转（环节四新增）—— <b>声明式事务的落点</b>。
     *
     * <p>{@code @Transactional} 的效果：本方法被 Spring 用 AOP 代理包了一层，
     * 进入方法前开启事务（autoCommit=false），正常返回则 commit，
     * 抛出 RuntimeException 则 rollback。业务代码里看不到任何
     * {@code setAutoCommit / commit / rollback} —— 这就是"声明式"的含义。
     *
     * <p><b>为什么这个场景真的需要事务</b>：
     * <pre>
     *   SELECT status ...   ← 读到"待办(0)"
     *        （此刻另一个请求把它改成了"进行中(1)"）
     *   UPDATE ... status=1 ← 覆盖别人的修改，业务规则被绕过
     * </pre>
     * 把两步放进同一个事务，配合合适的隔离级别（如 REPEATABLE READ）
     * 能让"读到的值"与"更新的值"保持一致，避免这类竞态。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(int id, String action) {
        // 与其它方法保持一致：把底层 DataAccessException 统一翻译成 ServiceException，
        // 这样 Web 层只需面对一种业务异常（否则"数据库宕机"时会漏出技术细节）。
        try {
            doChangeStatus(id, action);
        } catch (DataAccessException e) {
            throw new ServiceException("任务状态流转失败, id=" + id + ", action=" + action, e);
        }
    }

    /** 状态流转的实际逻辑：查当前状态 → 校验是否允许 → 更新。 */
    private void doChangeStatus(int id, String action) {
        Task task = taskMapper.findById(id);
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

        int rows = taskMapper.updateStatus(id, next);
        if (rows != 1) {
            // 抛异常 → 事务回滚（虽然这里只更新了一行，但语义上必须一致）
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
        if (task.getTitle().trim().length() > 255) {
            throw new ServiceException("任务名称长度不能超过 255 个字符");
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
