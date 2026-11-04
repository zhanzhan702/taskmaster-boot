package cn.edu.mju.task.mapper;

import cn.edu.mju.task.entity.Task;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 任务 Mapper（MyBatis）—— <b>环节三：替换 JDBC 六步法</b>。
 *
 * <p><b>「接口没有实现类为什么能跑？」</b>（本环节最核心的原理）
 * <ol>
 *   <li>Spring 启动时，{@code MapperScannerConfigurer} 扫描到本包下的接口；</li>
 *   <li>对每个接口调用 {@code MapperFactoryBean}，最终由 MyBatis 的
 *       {@code MapperProxyFactory} 生成一个<b>动态代理对象</b>
 *       （基于 JDK 的 {@code java.lang.reflect.Proxy}）；</li>
 *   <li>代理对象注册成 Spring Bean，Service 注入它就能调用；</li>
 *   <li>调用方法时，代理拦截下来，通过
 *       <b>接口全限定名 + 方法名</b> 去 {@code Configuration} 里找对应的
 *       {@code MappedStatement}（也就是 XML 里的 {@code <select id="...">} 或注解）；</li>
 *   <li>找到后执行 SQL，并用反射把 {@code ResultSet} 映射成返回值类型。</li>
 * </ol>
 * 所以"没有实现类"只是因为<b>实现被代理接管了</b>：
 * 方法名 = SQL 的 id，这两者必须严格对应。
 *
 * <p><b>写法分工</b>（与环节二的思路一致）：
 * 简单单条语句用注解就近写；需要 {@code resultMap}、{@code useGeneratedKeys}
 * 或多行 SQL 的放 XML。
 */
public interface TaskMapper {

    // ==================== XML 中定义（见 mapper/TaskMapper.xml）====================

    /** 查询全部任务，按 id 升序。 */
    List<Task> findAll();

    /** 新增任务；成功后自增主键回填到入参对象的 id 属性。 */
    int insert(Task task);

    /** 按主键更新。 */
    int update(Task task);

    // ==================== 注解式定义（简单语句就近写）====================

    /**
     * 按主键查询。
     *
     * <p>开启了 {@code mapUnderscoreToCamelCase}，所以
     * {@code due_date → dueDate}、{@code create_date → createDate} 自动映射。
     *
     * @return 不存在返回 {@code null}
     */
    @Select("SELECT id, title, status, priority, due_date, create_date "
            + "FROM t_task WHERE id = #{id}")
    Task findById(@Param("id") int id);

    /** 按主键删除，返回受影响行数。 */
    @Delete("DELETE FROM t_task WHERE id = #{id}")
    int deleteById(@Param("id") int id);

    /**
     * 只更新状态字段（环节四：状态流转）。
     *
     * <p>单独一条 UPDATE 而不是"查出整个对象再改再存"，有两个好处：
     * 不会把其它字段的旧值覆盖回去（避免丢失并发修改），
     * 且 SQL 更短更明确。
     */
    @Update("UPDATE t_task SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") int id, @Param("status") byte status);
}
