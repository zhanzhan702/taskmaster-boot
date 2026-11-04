<%--
  任务列表页（环节二重构版）。

  与环节一相比的改动：
    1. 数据源由 List<String[]> 换成 List<Task>，用 getter 取值，不再有 r[0] 这种下标；
    2. 状态/优先级显示中文（由 Task 的 getStatusLabel/getPriorityLabel 提供）；
    3. 日期由 getDueDateText 格式化，不再直接打印 Date；
    4. 标题经 WebUtil.escape 转义，修复环节一的存储型 XSS；
    5. 增加"暂无数据"空态，避免 rows 为 null 时空指针；
    6. 增加顶部操作区：新增、退出登录、当前用户。

  仍然存在的痛点：循环与条件只能用脚本片段（未引入 JSTL），
  且每处输出都要记得手工转义 —— 见《原生开发痛点清单》。
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="cn.edu.mju.task.entity.Task" %>
<%@ page import="cn.edu.mju.task.entity.User" %>
<%@ page import="cn.edu.mju.task.util.WebUtil" %>
<%
    String ctx = request.getContextPath();
    List<Task> tasks = (List<Task>) request.getAttribute("tasks");
    String msg = (String) request.getAttribute("msg");
    User loginUser = (User) session.getAttribute("loginUser");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>任务列表 · TaskMaster</title>
</head>
<body>
<%
    // 环境标识来自 application-{profile}.yml（TaskProperties），用于确认当前连的是哪个环境
    String envLabel = (String) request.getAttribute("envLabel");
    String systemName = (String) request.getAttribute("systemName");
%>
<h2>任务列表
    <% if (envLabel != null) { %>
        <span style="font-size:13px;color:#fff;background:#36c;padding:2px 6px;border-radius:3px;vertical-align:middle;">
            <%= WebUtil.escape(envLabel) %>
        </span>
    <% } %>
</h2>

<p>
    <% if (systemName != null) { %>系统：<%= WebUtil.escape(systemName) %> ｜ <% } %>
    当前用户：<strong><%= loginUser == null ? "未登录" : WebUtil.escape(loginUser.getNickname()) %></strong>
    ｜ <a href="<%=ctx%>/task/add">＋ 新增任务</a>
    ｜ <a href="<%=ctx%>/logout">退出登录</a>
</p>

<% if (msg != null) { %>
    <p style="color:#080;">✔ <%= WebUtil.escape(msg) %></p>
<% } %>

<%
    if (tasks == null || tasks.isEmpty()) {
%>
    <p>暂无任务数据，点击上方「新增任务」开始。</p>
<%
    } else {
%>
<table border="1" cellspacing="0" cellpadding="6">
    <tr>
        <th>编号</th>
        <th>任务名称</th>
        <th>状态</th>
        <th>优先级</th>
        <th>截止日期</th>
        <th>创建日期</th>
        <th>操作</th>
    </tr>
    <%
        for (Task task : tasks) {
    %>
    <tr>
        <td><%= task.getId() %></td>
        <td><%= WebUtil.escape(task.getTitle()) %></td>
        <td><%= task.getStatusLabel() %></td>
        <td><%= task.getPriorityLabel() %></td>
        <td><%= task.getDueDateText() %></td>
        <td><%= task.getCreateDateText() %></td>
        <td>
            <a href="<%=ctx%>/task/edit/<%= task.getId() %>">修改</a>

            <form action="<%=ctx%>/task/delete/<%= task.getId() %>" method="post" style="display:inline"
                  onsubmit="return confirm('确定要删除该任务吗？');">
                <button type="submit">删除</button>
            </form>

            <%-- 状态流转按钮：按当前状态只显示合法的那一个操作 --%>
            <%
                if (task.getStatus() != null) {
                    byte st = task.getStatus().byteValue();
                    String action;
                    String label;
                    if (st == 0) {
                        action = "start";
                        label = "开始";
                    } else if (st == 1) {
                        action = "complete";
                        label = "完成";
                    } else {
                        action = "reopen";
                        label = "重新打开";
                    }
            %>
            <form action="<%=ctx%>/task/status/<%= task.getId() %>" method="post" style="display:inline">
                <input type="hidden" name="action" value="<%= action %>"/>
                <button type="submit"><%= label %></button>
            </form>
            <%
                }
            %>
        </td>
    </tr>
    <%
        }
    %>
</table>
<%
    }
%>

</body>
</html>
