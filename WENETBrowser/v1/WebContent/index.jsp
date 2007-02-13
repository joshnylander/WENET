<%@ page language="java" contentType="text/html" %>
<%
response.sendRedirect("servlet/WENETBrowserServlet?act=Login&impl=Standard&uname=" + request.getRemoteUser());
%>