package cn.edu.mju.task.mapper;

import cn.edu.mju.task.entity.Task;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 任务 Mapper —— <b>实验五环节三：改用 MyBatis-Plus 的 BaseMapper</b>。
 *
 * <p>对比实验三/四：那时这里要声明 findById / deleteById / updateStatus 等方法，
 * 每个方法都要写注解 SQL 或 XML。现在<b>一行代码都不用写</b>，
 * 因为 {@link BaseMapper} 已经提供了 17 个通用方法：
 *
 * <pre>
 *   insert(T)                      selectById(Serializable)
 *   deleteById(Serializable)       selectList(Wrapper)
 *   updateById(T)                  selectPage(Page, Wrapper)
 *   selectCount(Wrapper)           selectOne(Wrapper) ...
 * </pre>
 *
 * <p><b>它凭什么能工作？</b>继承接口后，MyBatis-Plus 在启动时会为该接口注入
 * 一组 {@code MappedStatement}（方法名 → SQL 模板），仍然走 MyBatis 的动态代理。
 * 也就是说「接口无实现类」的原理没变，只是 SQL 由框架按实体的表结构自动生成。
 *
 * <p><b>代价（报告里会展开讨论）</b>：
 * <ul>
 *   <li>SQL 不可见 —— 不再有 XML 可以直接读，要看真实语句只能开日志；</li>
 *   <li>复杂查询会被迫退回 Wrapper 拼接，可读性未必比手写 SQL 好；</li>
 *   <li>自动生成的 SQL 可能与预期不符（例如全表更新风险、逻辑删除、
 *       字段更新策略），排查时需要 {@code mybatis-plus.configuration.log-impl}；</li>
 *   <li>关联查询不支持，仍是 N+1 的重灾区。</li>
 * </ul>
 *
 * <p>本项目仍保留了两个自定义方法（见下方），因为它们涉及「带条件的原子更新」，
 * 用 Wrapper 表达不如一行 SQL 清晰 —— 这正是「封装有边界」的实证。
 */
public interface TaskMapper extends BaseMapper<Task> {
}
