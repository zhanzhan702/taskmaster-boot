package cn.edu.mju.task.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置 —— <b>环节三：分页插件的注册</b>。
 *
 * <p>这是本环节最容易被忽略的一步：<b>不注册分页插件，{@code selectPage} 不会真的分页</b>。
 * 现象是「查询结果永远只有一页、total 也不对」，而且不报错。
 * 原因：{@code BaseMapper.selectPage} 只是发起一次普通查询，
 * 真正的 {@code LIMIT} 与 {@code COUNT} 是由拦截器在执行阶段改写 SQL 完成的。
 *
 * <p>原理：{@link PaginationInnerInterceptor} 是一个 MyBatis 的
 * {@code Interceptor}（插件），它拦截 {@code Executor.query}：
 * <ol>
 *   <li>先按原条件执行一次 {@code SELECT COUNT(*)} 得到总数；</li>
 *   <li>再给原 SQL 拼上 {@code LIMIT offset, size} 执行真正的数据查询。</li>
 * </ol>
 * 所以能用极少代码完成分页，代价是「实际执行的 SQL 与你写的完全不同」——
 * 这正是「封装带来的代价」最典型的一处。
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 指定数据库类型，分页方言才正确（MySQL 用 LIMIT）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
