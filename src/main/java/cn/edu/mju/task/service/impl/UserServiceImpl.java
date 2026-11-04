package cn.edu.mju.task.service.impl;

import cn.edu.mju.task.entity.User;
import cn.edu.mju.task.mapper.UserMapper;
import cn.edu.mju.task.service.ServiceException;
import cn.edu.mju.task.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/**
 * 用户业务实现 —— <b>仍采用 setter 注入</b>，只是依赖从 UserDao 换成了 UserMapper。
 *
 * <p>对照 {@link TaskServiceImpl} 的构造器注入：
 * 这里的依赖字段不能是 {@code final}，且必须有<b>无参构造器</b>，
 * 因为容器要先 new 对象、再调 {@code setUserMapper(...)}。
 *
 * <p>两种注入方式在 {@code applicationContext.xml} 里分别写成
 * {@code <constructor-arg>} 与 {@code <property>}，可以直观对照。
 */
@Service
public class UserServiceImpl implements UserService {

    /** setter 注入 → 不能是 final。 */
    private UserMapper userMapper;

    /** 无参构造器：setter 注入的前提。 */
    public UserServiceImpl() {
        // 容器会先调用它，再注入属性
    }

    /**
     * setter 注入（实验四：改由注解驱动）。
     *
     * <p>实验三时是 {@code applicationContext.xml} 里的
     * {@code <property name="userMapper" ref="userMapper"/>}；
     * 现在改为在 setter 上加 {@code @Autowired}。
     *
     * <p><b>注意这里必须显式写 {@code @Autowired}</b>：Spring 对 setter 不会像
     * 单构造器那样"隐式注入"。这也正好体现了构造器注入与 setter 注入的差别 ——
     * 前者是"创建对象的一部分"，后者是"创建之后的额外动作"。
     */
    @Autowired
    public void setUserMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public User login(String loginName, String loginPwd) {
        if (loginName == null || loginName.isEmpty()
                || loginPwd == null || loginPwd.isEmpty()) {
            return null;
        }
        try {
            User user = userMapper.findByLoginName(loginName);
            if (user == null) {
                return null;
            }
            return loginPwd.equals(user.getLoginPwd()) ? user : null;
        } catch (DataAccessException e) {
            throw new ServiceException("登录校验失败", e);
        }
    }
}
