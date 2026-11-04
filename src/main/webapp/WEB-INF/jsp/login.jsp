<%--
  登录页（环节四）：从 webapp 根目录移到 /WEB-INF/jsp/ 下。

  为什么要移：放在 WEB-INF 里浏览器无法直接访问，必须经 AuthController 转发，
  这样"进入登录页"的路径唯一，登录成功后的跳转逻辑也只有一处，不会被绕过。

  可用属性：
    error     登录失败提示
    notice    提示信息（如"您已安全退出"）
    loginName 回显上次输入的用户名（密码不回显）
    param.redirect  拦截器带过来的原始目标地址

  XSS：所有用户可控内容都经 WebUtil.escape 处理。
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="cn.edu.mju.task.util.WebUtil" %>
<%
    String ctx = request.getContextPath();
    String error = (String) request.getAttribute("error");
    String notice = (String) request.getAttribute("notice");
    String loginName = (String) request.getAttribute("loginName");
    String redirect = request.getParameter("redirect");
    // 以下三项来自 application.yml / application-{profile}.yml（TaskProperties）
    String systemName = (String) request.getAttribute("systemName");
    String envLabel = (String) request.getAttribute("envLabel");
    String welcomeMessage = (String) request.getAttribute("welcomeMessage");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>登录 · <%= systemName == null ? "TaskMaster" : WebUtil.escape(systemName) %></title>
</head>
<body>
<h2><%= systemName == null ? "TaskMaster" : WebUtil.escape(systemName) %> 登录</h2>

<p style="color:#666;">
    当前环境：<strong><%= envLabel == null ? "-" : WebUtil.escape(envLabel) %></strong>
    <% if (welcomeMessage != null && !welcomeMessage.isEmpty()) { %>
        ｜ <%= WebUtil.escape(welcomeMessage) %>
    <% } %>
</p>

<% if (notice != null) { %>
    <p style="color:#080;"><%= WebUtil.escape(notice) %></p>
<% } %>

<% if (error != null) { %>
    <p style="color:#c00;">⚠ <%= WebUtil.escape(error) %></p>
<% } %>

<form action="<%=ctx%>/login" method="post">
    <input type="hidden" name="redirect" value="<%= WebUtil.escape(redirect) %>"/>
    用户名：<input type="text" name="loginName" maxlength="50"
                 value="<%= WebUtil.escape(loginName) %>"/><br/>
    密　码：<input type="password" name="loginPwd" maxlength="50"/><br/>
    <input type="submit" value="登录"/>
</form>

<p style="color:#888;">测试账号：admin / 123456</p>
</body>
</html>
