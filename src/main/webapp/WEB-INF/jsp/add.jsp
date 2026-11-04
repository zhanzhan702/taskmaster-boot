<%--
  新增任务表单。

  痛点提示：本文件与 edit.jsp 的差异只有"标题文字"和"是否带 id 隐藏域"，
  其余 90% 完全重复。原生 JSP 没有模板继承/片段复用（需引入 JSTL 或自定义标签），
  只能复制粘贴 —— 见《原生开发痛点清单》第 9 条。
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="cn.edu.mju.task.entity.Task" %>
<%@ page import="cn.edu.mju.task.util.WebUtil" %>
<%
    String ctx = request.getContextPath();
    Task task = (Task) request.getAttribute("task");
    String error = (String) request.getAttribute("error");
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>新增任务 · TaskMaster</title>
</head>
<body>
<h2>新增任务</h2>

<% if (error != null) { %>
    <p style="color:#c00;">⚠ <%= WebUtil.escape(error) %></p>
<% } %>

<form action="<%=ctx%>/task/add" method="post">
    任务名称：<input type="text" name="title" maxlength="255"
                     value="<%= WebUtil.escape(task == null ? "" : task.getTitle()) %>"/><br/>

    优先级别：
    <select name="priority">
        <option value="1" <%= task != null && task.getPriority() != null && task.getPriority().byteValue() == 1 ? "selected" : "" %>>低</option>
        <option value="2" <%= task != null && task.getPriority() != null && task.getPriority().byteValue() == 2 ? "selected" : "" %>>中</option>
        <option value="3" <%= task != null && task.getPriority() != null && task.getPriority().byteValue() == 3 ? "selected" : "" %>>高</option>
    </select><br/>

    任务状态：
    <select name="status">
        <option value="0" <%= task != null && task.getStatus() != null && task.getStatus().byteValue() == 0 ? "selected" : "" %>>待办</option>
        <option value="1" <%= task != null && task.getStatus() != null && task.getStatus().byteValue() == 1 ? "selected" : "" %>>进行中</option>
        <option value="2" <%= task != null && task.getStatus() != null && task.getStatus().byteValue() == 2 ? "selected" : "" %>>已完成</option>
    </select><br/>

    截止日期：<input type="date" name="dueDate"
                     value="<%= task == null ? "" : task.getDueDateText() %>"/><br/>

    <input type="submit" value="提交"/>
    <a href="<%=ctx%>/task/list">返回列表</a>
</form>

</body>
</html>
