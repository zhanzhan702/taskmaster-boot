package cn.edu.mju.task.service;

import cn.edu.mju.task.entity.Task;
import cn.edu.mju.task.mapper.TaskMapper;
import cn.edu.mju.task.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link TaskServiceImpl} 的单元测试 —— <b>实验四环节三 + 环节四</b>。
 *
 * <h3>为什么这样设计</h3>
 * <ul>
 *   <li><b>只用 Mockito，不启动 Spring 容器</b>：{@code @ExtendWith(MockitoExtension.class)}
 *       不加载任何 ApplicationContext，因此测试可在无数据库、无网络的机器上运行；</li>
 *   <li><b>{@code @Mock TaskMapper} + {@code @InjectMocks TaskServiceImpl}</b>：
 *       Mockito 构造一个假的 Mapper 并通过构造器注入到被测对象，
 *       这就是"用假实现替换持久层"——实验四环节四的核心手法；</li>
 *   <li><b>不碰真实数据库</b>：所有 SQL 都被桩替代，测试跑完数据库里一条记录都不会变。</li>
 * </ul>
 *
 * <h3>覆盖的测试类型</h3>
 * <table border="1">
 *   <tr><th>类型</th><th>本类中的例子</th></tr>
 *   <tr><td>正常流</td><td>{@code findAll} 返回列表、{@code add} 成功写入</td></tr>
 *   <tr><td>异常流</td><td>数据库异常 → {@code ServiceException}、影响 0 行 → 抛异常</td></tr>
 *   <tr><td>边界值</td><td>标题恰好 255 字通过 / 256 字被拒、优先级 1 与 3 通过 / 0 与 4 被拒</td></tr>
 *   <tr><td>分支覆盖</td><td>{@code changeStatus} 的三条合法分支 + 六种非法组合 + 未知操作</td></tr>
 * </table>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl 业务层单元测试")
class TaskServiceImplTest {

    /** 假 Mapper：所有方法默认返回 0/null/false，需要时用 when(...) 打桩。 */
    @Mock
    private TaskMapper taskMapper;

    /** 被测对象：由 Mockito 通过构造器注入上面的假 Mapper。 */
    @InjectMocks
    private TaskServiceImpl taskService;

    // ==================== 测试数据构造 ====================

    /** 构造一个"各项都合法"的任务，供各用例作为基线，避免每个用例重复赋值。 */
    private static Task validTask() {
        Task t = new Task();
        t.setTitle("完成实验报告");
        t.setStatus(Byte.valueOf((byte) 0));
        t.setPriority(Byte.valueOf((byte) 2));
        t.setDueDate(new Date());
        return t;
    }

    /** 生成指定长度的标题，用于标题长度边界测试。 */
    private static String titleOfLength(int length) {
        char[] chars = new char[length];
        Arrays.fill(chars, 'x');
        return new String(chars);
    }

    private static Task taskWithStatus(byte status) {
        Task t = validTask();
        t.setStatus(Byte.valueOf(status));
        t.setId(Integer.valueOf(7));
        return t;
    }

    // ====================================================================
    @Nested
    @DisplayName("查询：findAll / findById")
    class QueryTests {

        @Test
        @DisplayName("findAll 应原样返回 Mapper 的结果")
        void findAll_shouldReturnMapperResult() {
            List<Task> expected = Arrays.asList(validTask(), validTask());
            when(taskMapper.findAll()).thenReturn(expected);

            List<Task> actual = taskService.findAll();

            assertThat(actual).hasSize(2).isSameAs(expected);
            verify(taskMapper, times(1)).findAll();
        }

        @Test
        @DisplayName("findAll 遇到空表应返回空列表而不是 null")
        void findAll_shouldReturnEmptyListWhenNoData() {
            when(taskMapper.findAll()).thenReturn(Collections.<Task>emptyList());

            assertThat(taskService.findAll()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("findById 找到记录时返回实体")
        void findById_shouldReturnTask() {
            Task task = validTask();
            task.setId(Integer.valueOf(3));
            when(taskMapper.findById(3)).thenReturn(task);

            Task actual = taskService.findById(3);

            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo(3);
            verify(taskMapper).findById(3);
        }

        @Test
        @DisplayName("findById 记录不存在时返回 null（不抛异常）")
        void findById_shouldReturnNullWhenAbsent() {
            when(taskMapper.findById(999)).thenReturn(null);

            assertThat(taskService.findById(999)).isNull();
        }

        @Test
        @DisplayName("findAll 数据库异常应翻译成 ServiceException")
        void findAll_shouldWrapDataAccessException() {
            when(taskMapper.findAll())
                    .thenThrow(new DataAccessResourceFailureException("数据库连接失败"));

            assertThatThrownBy(() -> taskService.findAll())
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("查询任务列表失败")
                    .hasCauseInstanceOf(DataAccessResourceFailureException.class);
        }

        @Test
        @DisplayName("findById 数据库异常应翻译成 ServiceException")
        void findById_shouldWrapDataAccessException() {
            when(taskMapper.findById(3))
                    .thenThrow(new DataAccessResourceFailureException("连接中断"));

            assertThatThrownBy(() -> taskService.findById(3))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("查询任务失败")
                    .hasCauseInstanceOf(DataAccessResourceFailureException.class);
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("构造器：依赖不可为 null")
    class ConstructorTests {

        @Test
        @DisplayName("传入 null 的 Mapper 应立刻失败（构造器注入的防御价值）")
        void constructor_shouldRejectNullMapper() {
            assertThatThrownBy(() -> new TaskServiceImpl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taskMapper 不能为 null");
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("新增：add 的校验与写入")
    class AddTests {

        @Test
        @DisplayName("合法任务应写入一次，并把原对象传给 Mapper")
        void add_shouldInsertOnce() {
            Task task = validTask();
            when(taskMapper.insert(any(Task.class))).thenReturn(1);

            taskService.add(task);

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskMapper, times(1)).insert(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("完成实验报告");
        }

        @ParameterizedTest(name = "标题为 [{0}] 应被拒绝")
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("标题为空或纯空白应被拒绝，且不触碰 Mapper")
        void add_shouldRejectBlankTitle(String title) {
            Task task = validTask();
            task.setTitle(title);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务名称不能为空");

            // 关键断言：校验失败时绝不能访问数据库
            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("边界：标题恰好 255 字应通过")
        void add_shouldAcceptTitleOf255() {
            Task task = validTask();
            task.setTitle(titleOfLength(255));
            when(taskMapper.insert(any(Task.class))).thenReturn(1);

            taskService.add(task);

            verify(taskMapper).insert(task);
        }

        @Test
        @DisplayName("边界：标题 256 字应被拒绝")
        void add_shouldRejectTitleOf256() {
            Task task = validTask();
            task.setTitle(titleOfLength(256));

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("长度不能超过 255");

            verifyNoInteractions(taskMapper);
        }

        @ParameterizedTest(name = "优先级 {0} 应被拒绝")
        @ValueSource(ints = {-1, 0, 4, 99})
        @DisplayName("优先级越界（合法区间 1~3）应被拒绝")
        void add_shouldRejectIllegalPriority(int priority) {
            Task task = validTask();
            task.setPriority(Byte.valueOf((byte) priority));

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("优先级取值应为 1~3");

            verifyNoInteractions(taskMapper);
        }

        @ParameterizedTest(name = "优先级 {0} 应通过")
        @ValueSource(ints = {1, 2, 3})
        @DisplayName("优先级边界内取值（1/2/3）应通过")
        void add_shouldAcceptLegalPriority(int priority) {
            Task task = validTask();
            task.setPriority(Byte.valueOf((byte) priority));
            when(taskMapper.insert(any(Task.class))).thenReturn(1);

            taskService.add(task);

            verify(taskMapper).insert(task);
        }

        @ParameterizedTest(name = "状态 {0} 应被拒绝")
        @ValueSource(ints = {-1, 3, 10})
        @DisplayName("状态越界（合法区间 0~2）应被拒绝")
        void add_shouldRejectIllegalStatus(int status) {
            Task task = validTask();
            task.setStatus(Byte.valueOf((byte) status));

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务状态取值应为 0~2");

            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("截止日期为空应被拒绝")
        void add_shouldRejectNullDueDate() {
            Task task = validTask();
            task.setDueDate(null);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("截止日期不能为空");

            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("入参为 null 应被拒绝")
        void add_shouldRejectNullTask() {
            assertThatThrownBy(() -> taskService.add(null))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务对象不能为空");

            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("优先级为 null 应被拒绝（区别于越界，这是「缺失」）")
        void add_shouldRejectNullPriority() {
            Task task = validTask();
            task.setPriority(null);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("优先级取值应为 1~3");

            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("状态为 null 应被拒绝")
        void add_shouldRejectNullStatus() {
            Task task = validTask();
            task.setStatus(null);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务状态取值应为 0~2");

            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("数据库异常应翻译成 ServiceException")
        void add_shouldWrapDataAccessException() {
            Task task = validTask();
            when(taskMapper.insert(any(Task.class)))
                    .thenThrow(new DataAccessResourceFailureException("数据库宕机"));

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("新增任务失败")
                    .hasCauseInstanceOf(DataAccessResourceFailureException.class);
        }

        @Test
        @DisplayName("Mapper 返回 0 行说明未被写入，应抛异常")
        void add_shouldFailWhenNoRowAffected() {
            Task task = validTask();
            when(taskMapper.insert(any(Task.class))).thenReturn(0);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("受影响行数为 0");
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("修改：update")
    class UpdateTests {

        @Test
        @DisplayName("合法任务应更新一次")
        void update_shouldUpdateOnce() {
            Task task = validTask();
            task.setId(Integer.valueOf(5));
            when(taskMapper.update(task)).thenReturn(1);

            taskService.update(task);

            verify(taskMapper).update(task);
        }

        @Test
        @DisplayName("缺少 id 应被拒绝")
        void update_shouldRejectNullId() {
            Task task = validTask();
            task.setId(null);

            assertThatThrownBy(() -> taskService.update(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("必须提供 id");

            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("影响 0 行（记录已被删除）应抛异常")
        void update_shouldFailWhenRecordAbsent() {
            Task task = validTask();
            task.setId(Integer.valueOf(5));
            when(taskMapper.update(task)).thenReturn(0);

            assertThatThrownBy(() -> taskService.update(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("要修改的任务不存在");
        }

        @Test
        @DisplayName("数据库异常应翻译成 ServiceException")
        void update_shouldWrapDataAccessException() {
            Task task = validTask();
            task.setId(Integer.valueOf(5));
            when(taskMapper.update(task))
                    .thenThrow(new DataAccessResourceFailureException("连接中断"));

            assertThatThrownBy(() -> taskService.update(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("更新任务失败")
                    .hasCauseInstanceOf(DataAccessResourceFailureException.class);
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("删除：delete")
    class DeleteTests {

        @Test
        @DisplayName("应按 id 删除一次")
        void delete_shouldDeleteOnce() {
            when(taskMapper.deleteById(5)).thenReturn(1);

            taskService.delete(5);

            verify(taskMapper).deleteById(5);
        }

        @Test
        @DisplayName("影响 0 行说明记录不存在，应抛异常")
        void delete_shouldFailWhenAbsent() {
            when(taskMapper.deleteById(5)).thenReturn(0);

            assertThatThrownBy(() -> taskService.delete(5))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("要删除的任务不存在");
        }

        @Test
        @DisplayName("数据库异常应翻译成 ServiceException")
        void delete_shouldWrapDataAccessException() {
            when(taskMapper.deleteById(5))
                    .thenThrow(new DataAccessResourceFailureException("连接中断"));

            assertThatThrownBy(() -> taskService.delete(5))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("删除任务失败");
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("状态流转：changeStatus")
    class ChangeStatusTests {

        @ParameterizedTest(name = "{0}：状态 {1} -> {2}")
        @CsvSource({
                "start,    0, 1",
                "complete, 1, 2",
                "reopen,   2, 1"
        })
        @DisplayName("三条合法流转应更新为下一状态")
        void changeStatus_shouldUpdateOnLegalTransition(String action, byte fromStatus, byte toStatus) {
            when(taskMapper.findById(7)).thenReturn(taskWithStatus(fromStatus));
            when(taskMapper.updateStatus(7, toStatus)).thenReturn(1);

            taskService.changeStatus(7, action);

            verify(taskMapper).updateStatus(7, toStatus);
        }

        @ParameterizedTest(name = "{0}：从状态 {1} 出发应被拒绝")
        @CsvSource({
                "start,    1",
                "start,    2",
                "complete, 0",
                "complete, 2",
                "reopen,   0",
                "reopen,   1"
        })
        @DisplayName("六种非法流转应被拒绝，且绝不能写库")
        void changeStatus_shouldRejectIllegalTransition(String action, byte fromStatus) {
            when(taskMapper.findById(7)).thenReturn(taskWithStatus(fromStatus));

            assertThatThrownBy(() -> taskService.changeStatus(7, action))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("只有「");

            // 这是状态机最重要的保证：非法流转不能产生任何写操作
            verify(taskMapper, never()).updateStatus(anyInt(), anyByte());
        }

        @Test
        @DisplayName("任务不存在应抛异常")
        void changeStatus_shouldFailWhenTaskAbsent() {
            when(taskMapper.findById(404)).thenReturn(null);

            assertThatThrownBy(() -> taskService.changeStatus(404, "start"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务不存在");

            verify(taskMapper, never()).updateStatus(anyInt(), anyByte());
        }

        @ParameterizedTest(name = "未知操作 [{0}] 应被拒绝")
        @NullAndEmptySource
        @ValueSource(strings = {"delete", "START", "start ", "开始"})
        @DisplayName("未知或大小写不符的 action 应被拒绝")
        void changeStatus_shouldRejectUnknownAction(String action) {
            when(taskMapper.findById(7)).thenReturn(taskWithStatus((byte) 0));

            assertThatThrownBy(() -> taskService.changeStatus(7, action))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("未知的状态操作");

            verify(taskMapper, never()).updateStatus(anyInt(), anyByte());
        }

        @Test
        @DisplayName("更新影响 0 行应抛异常")
        void changeStatus_shouldFailWhenNoRowAffected() {
            when(taskMapper.findById(7)).thenReturn(taskWithStatus((byte) 0));
            when(taskMapper.updateStatus(7, (byte) 1)).thenReturn(0);

            assertThatThrownBy(() -> taskService.changeStatus(7, "start"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("状态更新失败");
        }

        @Test
        @DisplayName("数据库宕机时应抛 ServiceException 而不是泄漏技术异常")
        void changeStatus_shouldWrapDataAccessException() {
            when(taskMapper.findById(7))
                    .thenThrow(new DataAccessResourceFailureException("数据库宕机"));

            assertThatThrownBy(() -> taskService.changeStatus(7, "start"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务状态流转失败")
                    .hasCauseInstanceOf(DataAccessResourceFailureException.class);
        }

        @Test
        @DisplayName("历史数据 status 为 null 时应按「待办」处理，允许 start")
        void changeStatus_shouldTreatNullStatusAsTodo() {
            Task task = taskWithStatus((byte) 0);
            task.setStatus(null);          // 模拟历史脏数据
            when(taskMapper.findById(7)).thenReturn(task);
            when(taskMapper.updateStatus(7, (byte) 1)).thenReturn(1);

            taskService.changeStatus(7, "start");

            verify(taskMapper).updateStatus(7, (byte) 1);
        }
    }
}
