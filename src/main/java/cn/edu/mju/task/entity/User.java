package cn.edu.mju.task.entity;

import java.io.Serializable;

/**
 * 用户实体（JavaBean）—— 用于登录验证。
 *
 * <p><b>注意</b>：本阶段密码以<b>明文</b>存储与比较。
 * 这是"原生开发"阶段刻意保留的简化，已列入痛点清单（安全性）。
 * 正确做法是加盐哈希（如 BCrypt），而那通常由安全框架承担。
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String loginName;
    private String loginPwd;
    private String nickname;

    public User() {
    }

    public User(Integer id, String loginName, String loginPwd, String nickname) {
        this.id = id;
        this.loginName = loginName;
        this.loginPwd = loginPwd;
        this.nickname = nickname;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getLoginPwd() {
        return loginPwd;
    }

    public void setLoginPwd(String loginPwd) {
        this.loginPwd = loginPwd;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 刻意不输出密码，避免进入日志。 */
    @Override
    public String toString() {
        return "User{id=" + id + ", loginName='" + loginName + "', nickname='" + nickname + "'}";
    }
}
