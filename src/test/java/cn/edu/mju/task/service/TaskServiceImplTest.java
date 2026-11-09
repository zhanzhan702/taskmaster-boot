package cn.edu.mju.task.service;

import cn.edu.mju.task.entity.Task;
import cn.edu.mju.task.mapper.TaskMapper;
import cn.edu.mju.task.service.impl.TaskServiceImpl;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link TaskServiceImpl} 单元测试 —— <b>实验五（MyBatis-Plus 版）</b>。
 *
 * <h3>与实验四的测试相比，本类有三处必须改</h3>
 * <ol>
 *   <li>被测方法从 {@code findAll()} 变成 {@code search(title, status, pageNum, pageSize)}，
 *       返回值从 {@code List} 变成 {@link IPage}；</li>
 *   <li>{@code TaskMapper} 现在继承 {@code BaseMapper}，方法名变成
 *       {@code selectPage / selectById / updateById / deleteById} ——
 *       这是「换持久层框架会波及测试」的直接证据；</li>
 *   <li>分页不再需要 stub 两次（{@code findByQuery} + {@code countByQuery}），
 *       插件的 COUNT 由框架内部完成，测试里只能看到一次 {@code selectPage}。</li>
 * </ol>
 *
 * <h3>仍然不依赖数据库</h3>
 * 全部用 Mockito 假 Mapper，{@code mvn test} 不需要 MySQL 在运行。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskServiceImpl 单元测试（MyBatis-Plus 版）")
class TaskServiceImplTest {

    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    // ==================== 测试数据构造 ====================

    private static Task validTask() {
        Task t = new Task();
        t.setTitle("完成实验报告");
        t.setStatus(Byte.valueOf((byte) 0));
        t.setPriority(Byte.valueOf((byte) 2));
        t.setDueDate(new Date());
        return t;
    }

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

    /** 构造一个「返回给定数据、给定总数」的分页结果，供 selectPage 打桩。 */
    private static Page<Task> pageOf(List<Task> records, long total, long current, long size) {
        Page<Task> page = new Page<Task>(current, size);
        page.setRecords(records);
        page.setTotal(total);
        return page;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void stubSelectPage(Page<Task> result) {
        when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(result);
    }

    // ====================================================================
    @Nested
    @DisplayName("条件查询 + 分页：search")
    class SearchTests {

        @Test
        @DisplayName("无条件时应返回当前页数据与总数")
        void search_shouldReturnPage() {
            List<Task> rows = Arrays.asList(validTask(), validTask());
            stubSelectPage(pageOf(rows, 12L, 1L, 5L));

            IPage<Task> result = taskService.search(null, null, 1, 5);

            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getTotal()).isEqualTo(12L);
            assertThat(result.getPages()).isEqualTo(3L);
            verify(taskMapper, times(1)).selectPage(any(Page.class), any(Wrapper.class));
        }

        @Test
        @DisplayName("带条件时应把分页参数原样交给 Mapper")
        void search_shouldPassPageParameters() {
            stubSelectPage(pageOf(Collections.<Task>emptyList(), 0L, 2L, 5L));

            taskService.search("报告", Byte.valueOf((byte) 0), 2, 5);

            ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
            verify(taskMapper).selectPage(captor.capture(), any(Wrapper.class));
            assertThat(captor.getValue().getCurrent()).isEqualTo(2L);
            assertThat(captor.getValue().getSize()).isEqualTo(5L);
        }

        @Test
        @DisplayName("页码小于 1 时应修正为 1（URL 参数不可信）")
        void search_shouldFixIllegalPageNum() {
            stubSelectPage(pageOf(Collections.<Task>emptyList(), 0L, 1L, 5L));

            taskService.search(null, null, 0, 5);

            ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
            verify(taskMapper).selectPage(captor.capture(), any(Wrapper.class));
            assertThat(captor.getValue().getCurrent()).isEqualTo(1L);
        }

        @Test
        @DisplayName("每页条数超上限时应回退为默认值，防止一次拉整表")
        void search_shouldClampPageSize() {
            stubSelectPage(pageOf(Collections.<Task>emptyList(), 0L, 1L, 10L));

            taskService.search(null, null, 1, 999999);

            ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
            verify(taskMapper).selectPage(captor.capture(), any(Wrapper.class));
            assertThat(captor.getValue().getSize()).isEqualTo(10L);
        }

        @Test
        @DisplayName("每页条数为 0 或负数时也应回退为默认值")
        void search_shouldClampNonPositivePageSize() {
            stubSelectPage(pageOf(Collections.<Task>emptyList(), 0L, 1L, 10L));

            taskService.search(null, null, 1, 0);

            ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);
            verify(taskMapper).selectPage(captor.capture(), any(Wrapper.class));
            assertThat(captor.getValue().getSize()).isEqualTo(10L);
        }

        @Test
        @DisplayName("关键字为空白串时不应参与模糊匹配（视为无条件）")
        void search_shouldIgnoreBlankKeyword() {
            stubSelectPage(pageOf(Collections.<Task>emptyList(), 0L, 1L, 5L));

            IPage<Task> result = taskService.search("   ", null, 1, 5);

            assertThat(result).isNotNull();
            verify(taskMapper).selectPage(any(Page.class), any(Wrapper.class));
        }

        @Test
        @DisplayName("关键字为 null 时也不应参与模糊匹配")
        void search_shouldHandleNullKeyword() {
            stubSelectPage(pageOf(Collections.<Task>emptyList(), 0L, 1L, 5L));

            taskService.search(null, Byte.valueOf((byte) 1), 1, 5);

            verify(taskMapper).selectPage(any(Page.class), any(Wrapper.class));
        }

        @Test
        @DisplayName("页码越界（删光了最后一页）应自动回退到最后一页")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void search_shouldFallbackWhenPageOutOfRange() {
            Page<Task> first = pageOf(Collections.<Task>emptyList(), 100L, 99L, 5L); // total=100 → 20 页
            Page<Task> last = pageOf(Arrays.asList(validTask()), 100L, 20L, 5L);

            when(taskMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(first, last);

            IPage<Task> result = taskService.search(null, null, 99, 5);

            assertThat(result.getCurrent()).isEqualTo(20L);
            verify(taskMapper, times(2)).selectPage(any(Page.class), any(Wrapper.class));
        }

        @Test
        @DisplayName("数据库异常应翻译成 ServiceException")
        @SuppressWarnings({"unchecked", "rawtypes"})
        void search_shouldWrapDataAccessException() {
            when(taskMapper.selectPage(any(Page.class), any(Wrapper.class)))
                    .thenThrow(new DataAccessResourceFailureException("数据库连接失败"));

            assertThatThrownBy(() -> taskService.search(null, null, 1, 5))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("查询任务列表失败")
                    .hasCauseInstanceOf(DataAccessResourceFailureException.class);
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("构造器：依赖不可为 null")
    class ConstructorTests {

        @Test
        @DisplayName("传入 null 的 Mapper 应立刻失败")
        void constructor_shouldRejectNullMapper() {
            assertThatThrownBy(() -> new TaskServiceImpl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("taskMapper 不能为 null");
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("查询详情：findById")
    class FindByIdTests {

        @Test
        @DisplayName("查到记录时返回实体")
        void findById_shouldReturnTask() {
            Task task = validTask();
            task.setId(Integer.valueOf(3));
            when(taskMapper.selectById(3)).thenReturn(task);

            assertThat(taskService.findById(3).getId()).isEqualTo(3);
        }

        @Test
        @DisplayName("记录不存在时返回 null")
        void findById_shouldReturnNullWhenAbsent() {
            when(taskMapper.selectById(999)).thenReturn(null);

            assertThat(taskService.findById(999)).isNull();
        }

        @Test
        @DisplayName("数据库异常应翻译成 ServiceException")
        void findById_shouldWrapDataAccessException() {
            when(taskMapper.selectById(3))
                    .thenThrow(new DataAccessResourceFailureException("连接中断"));

            assertThatThrownBy(() -> taskService.findById(3))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("查询任务失败");
        }
    }

    // ====================================================================
    @Nested
    @DisplayName("新增：add")
    class AddTests {

        @Test
        @DisplayName("合法任务应写入一次")
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
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("标题为空/纯空白应被拒绝且不触碰数据库")
        void add_shouldRejectBlankTitle(String title) {
            Task task = validTask();
            task.setTitle(title);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务名称不能为空");

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
        @DisplayName("优先级越界应被拒绝")
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
        @DisplayName("优先级边界内取值应通过")
        void add_shouldAcceptLegalPriority(int priority) {
            Task task = validTask();
            task.setPriority(Byte.valueOf((byte) priority));
            when(taskMapper.insert(any(Task.class))).thenReturn(1);

            taskService.add(task);

            verify(taskMapper).insert(task);
        }

        @ParameterizedTest(name = "状态 {0} 应被拒绝")
        @ValueSource(ints = {-1, 3, 10})
        @DisplayName("状态越界应被拒绝")
        void add_shouldRejectIllegalStatus(int status) {
            Task task = validTask();
            task.setStatus(Byte.valueOf((byte) status));

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务状态取值应为 0~2");

            verifyNoInteractions(taskMapper);
        }

        @Test
        @DisplayName("优先级为 null 应被拒绝")
        void add_shouldRejectNullPriority() {
            Task task = validTask();
            task.setPriority(null);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("优先级取值应为 1~3");
        }

        @Test
        @DisplayName("状态为 null 应被拒绝")
        void add_shouldRejectNullStatus() {
            Task task = validTask();
            task.setStatus(null);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务状态取值应为 0~2");
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
        @DisplayName("影响 0 行应抛异常")
        void add_shouldFailWhenNoRowAffected() {
            Task task = validTask();
            when(taskMapper.insert(any(Task.class))).thenReturn(0);

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("受影响行数为 0");
        }

        @Test
        @DisplayName("数据库异常应翻译成 ServiceException")
        void add_shouldWrapDataAccessException() {
            Task task = validTask();
            when(taskMapper.insert(any(Task.class)))
                    .thenThrow(new DataAccessResourceFailureException("数据库宕机"));

            assertThatThrownBy(() -> taskService.add(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("新增任务失败");
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
            when(taskMapper.updateById(task)).thenReturn(1);

            taskService.update(task);

            verify(taskMapper).updateById(task);
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
        @DisplayName("影响 0 行（记录已不存在）应抛异常")
        void update_shouldFailWhenRecordAbsent() {
            Task task = validTask();
            task.setId(Integer.valueOf(5));
            when(taskMapper.updateById(task)).thenReturn(0);

            assertThatThrownBy(() -> taskService.update(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("要修改的任务不存在");
        }

        @Test
        @DisplayName("数据库异常应翻译成 ServiceException")
        void update_shouldWrapDataAccessException() {
            Task task = validTask();
            task.setId(Integer.valueOf(5));
            when(taskMapper.updateById(task))
                    .thenThrow(new DataAccessResourceFailureException("连接中断"));

            assertThatThrownBy(() -> taskService.update(task))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("更新任务失败");
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
        @CsvSource({"start,    0, 1", "complete, 1, 2", "reopen,   2, 1"})
        @DisplayName("三条合法流转应更新为下一状态")
        void changeStatus_shouldUpdateOnLegalTransition(String action, byte fromStatus, byte toStatus) {
            when(taskMapper.selectById(7)).thenReturn(taskWithStatus(fromStatus));
            when(taskMapper.updateById(any(Task.class))).thenReturn(1);

            taskService.changeStatus(7, action);

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskMapper).updateById(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(7);
            assertThat(captor.getValue().getStatus()).isEqualTo(Byte.valueOf(toStatus));
        }

        @ParameterizedTest(name = "{0}：从状态 {1} 出发应被拒绝")
        @CsvSource({"start,    1", "start,    2", "complete, 0",
                "complete, 2", "reopen,   0", "reopen,   1"})
        @DisplayName("六种非法流转应被拒绝且绝不能写库")
        void changeStatus_shouldRejectIllegalTransition(String action, byte fromStatus) {
            when(taskMapper.selectById(7)).thenReturn(taskWithStatus(fromStatus));

            assertThatThrownBy(() -> taskService.changeStatus(7, action))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("只有「");

            verify(taskMapper, never()).updateById(any(Task.class));
        }

        @Test
        @DisplayName("任务不存在应抛异常")
        void changeStatus_shouldFailWhenTaskAbsent() {
            when(taskMapper.selectById(404)).thenReturn(null);

            assertThatThrownBy(() -> taskService.changeStatus(404, "start"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务不存在");

            verify(taskMapper, never()).updateById(any(Task.class));
        }

        @ParameterizedTest(name = "未知操作 [{0}] 应被拒绝")
        @NullAndEmptySource
        @ValueSource(strings = {"delete", "START", "start ", "开始"})
        @DisplayName("未知或大小写不符的 action 应被拒绝")
        void changeStatus_shouldRejectUnknownAction(String action) {
            when(taskMapper.selectById(7)).thenReturn(taskWithStatus((byte) 0));

            assertThatThrownBy(() -> taskService.changeStatus(7, action))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("未知的状态操作");

            verify(taskMapper, never()).updateById(any(Task.class));
        }

        @Test
        @DisplayName("更新影响 0 行应抛异常")
        void changeStatus_shouldFailWhenNoRowAffected() {
            when(taskMapper.selectById(7)).thenReturn(taskWithStatus((byte) 0));
            when(taskMapper.updateById(any(Task.class))).thenReturn(0);

            assertThatThrownBy(() -> taskService.changeStatus(7, "start"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("状态更新失败");
        }

        @Test
        @DisplayName("数据库宕机时应抛 ServiceException")
        void changeStatus_shouldWrapDataAccessException() {
            when(taskMapper.selectById(7))
                    .thenThrow(new DataAccessResourceFailureException("数据库宕机"));

            assertThatThrownBy(() -> taskService.changeStatus(7, "start"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("任务状态流转失败");
        }

        @Test
        @DisplayName("历史数据 status 为 null 时应按「待办」处理")
        void changeStatus_shouldTreatNullStatusAsTodo() {
            Task task = taskWithStatus((byte) 0);
            task.setStatus(null);
            when(taskMapper.selectById(7)).thenReturn(task);
            when(taskMapper.updateById(any(Task.class))).thenReturn(1);

            taskService.changeStatus(7, "start");

            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(taskMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(Byte.valueOf((byte) 1));
        }
    }
}
