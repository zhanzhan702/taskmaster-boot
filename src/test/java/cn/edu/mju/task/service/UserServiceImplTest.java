package cn.edu.mju.task.service;

import cn.edu.mju.task.entity.User;
import cn.edu.mju.task.mapper.UserMapper;
import cn.edu.mju.task.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link UserServiceImpl} 的单元测试。
 *
 * <p>本类刻意<b>不用</b> {@code @InjectMocks}，而是在 {@code @BeforeEach} 里
 * 手工 new + 调 setter —— 因为 {@code UserServiceImpl} 用的是 <b>setter 注入</b>。
 * 这正好演示了两种注入方式在测试代码里的差别：
 * <ul>
 *   <li>构造器注入（{@code TaskServiceImpl}）→ {@code @InjectMocks} 直接可用；</li>
 *   <li>setter 注入（{@code UserServiceImpl}）→ 需要先 new、再手动 set。</li>
 * </ul>
 *
 * <p>{@code @BeforeEach} 保证每个用例拿到<b>全新的被测对象</b>（JUnit 5 默认每个
 * 测试方法新建一个测试类实例），因此用例之间互不污染、可独立运行。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 登录校验单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // setter 注入：先建对象，再注入假 Mapper
        userService = new UserServiceImpl();
        userService.setUserMapper(userMapper);
    }

    private static User admin() {
        return new User(1, "admin", "123456", "管理员");
    }

    @Nested
    @DisplayName("登录成功")
    class SuccessTests {

        @Test
        @DisplayName("用户名与密码都正确时应返回用户对象")
        void login_shouldReturnUserWhenCredentialsMatch() {
            when(userMapper.findByLoginName("admin")).thenReturn(admin());

            User user = userService.login("admin", "123456");

            assertThat(user).isNotNull();
            assertThat(user.getLoginName()).isEqualTo("admin");
            assertThat(user.getNickname()).isEqualTo("管理员");
            verify(userMapper).findByLoginName("admin");
        }
    }

    @Nested
    @DisplayName("登录失败")
    class FailureTests {

        @Test
        @DisplayName("用户不存在时应返回 null")
        void login_shouldReturnNullWhenUserAbsent() {
            when(userMapper.findByLoginName("nobody")).thenReturn(null);

            assertThat(userService.login("nobody", "123456")).isNull();
        }

        @Test
        @DisplayName("密码错误时应返回 null（且不泄漏「用户存在」这一信息）")
        void login_shouldReturnNullWhenPasswordWrong() {
            when(userMapper.findByLoginName("admin")).thenReturn(admin());

            assertThat(userService.login("admin", "wrong-password")).isNull();
        }

        @Test
        @DisplayName("密码大小写敏感：123456 与 123456 之外的一律拒绝")
        void login_shouldBeCaseSensitiveOnPassword() {
            when(userMapper.findByLoginName("admin")).thenReturn(admin());

            assertThat(userService.login("admin", "123456 ")).isNull();
            assertThat(userService.login("admin", " 123456")).isNull();
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @ParameterizedTest(name = "用户名 [{0}] 为空应直接返回 null")
        @NullAndEmptySource
        @DisplayName("用户名为空时不查库")
        void login_shouldRejectBlankLoginName(String loginName) {
            assertThat(userService.login(loginName, "123456")).isNull();

            // 空参数属于明显非法请求，不应该浪费一次数据库往返
            verifyNoInteractions(userMapper);
        }

        @ParameterizedTest(name = "密码 [{0}] 为空应直接返回 null")
        @NullAndEmptySource
        @DisplayName("密码为空时不查库")
        void login_shouldRejectBlankPassword(String loginPwd) {
            assertThat(userService.login("admin", loginPwd)).isNull();

            verifyNoInteractions(userMapper);
        }
    }

    @Nested
    @DisplayName("依赖隔离")
    class IsolationTests {

        @Test
        @DisplayName("数据库宕机时应抛 ServiceException 而不是泄漏 DataAccessException")
        void login_shouldWrapDataAccessException() {
            when(userMapper.findByLoginName("admin"))
                    .thenThrow(new DataAccessResourceFailureException("数据库宕机"));

            assertThatThrownBy(() -> userService.login("admin", "123456"))
                    .isInstanceOf(ServiceException.class)
                    .hasMessageContaining("登录校验失败")
                    .hasCauseInstanceOf(DataAccessResourceFailureException.class);
        }
    }
}
