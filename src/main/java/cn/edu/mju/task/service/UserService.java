package cn.edu.mju.task.service;

import cn.edu.mju.task.entity.User;

/**
 * 用户业务接口（登录用）。
 *
 * <p>本接口的实现类刻意采用 <b>setter 注入</b>，与 {@link TaskService} 的
 * 构造器注入形成对照，放在 {@code applicationContext.xml} 里一眼能看出两种写法的差别。
 */
public interface UserService {

    /**
     * 校验登录。
     *
     * @return 校验通过返回用户对象；用户名不存在或密码不符返回 {@code null}
     */
    User login(String loginName, String loginPwd);
}
