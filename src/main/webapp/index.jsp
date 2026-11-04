<%--
  首页：直接跳到任务列表。
  环节一还没有登录，所以进来就能看列表。
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%
    response.sendRedirect(request.getContextPath() + "/task/list");
%>
