package cn.edu.mju.task.mapper;

import cn.edu.mju.task.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper（登录校验用）。
 *
 * <p>同样由 MyBatis 动态代理实现，没有实现类。
 */
public interface UserMapper {

    /**
     * 按登录名查询用户。
     *
     * <p>{@code login_name → loginName}、{@code login_pwd → loginPwd}
     * 由驼峰映射自动完成。
     *
     * @return 不存在返回 {@code null}
     */
    @Select("SELECT id, login_name, login_pwd, nickname FROM t_user WHERE login_name = #{loginName}")
    User findByLoginName(@Param("loginName") String loginName);
}
