package cn.edu.mju.task.mapper;

import cn.edu.mju.task.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 用户 Mapper —— 实验五改为继承 {@link BaseMapper}。
 *
 * <p>实验三/四里的 {@code findByLoginName} 自定义方法被删除了：
 * 一条等值查询用 {@code LambdaQueryWrapper} 表达即可，不必再占一个 Mapper 方法：
 * <pre>
 *   userMapper.selectOne(Wrappers.&lt;User&gt;lambdaQuery()
 *           .eq(User::getLoginName, loginName));
 * </pre>
 * 代价是「这条 SQL 长什么样」需要靠调用点推断，而不是一眼看到。
 */
public interface UserMapper extends BaseMapper<User> {
}
