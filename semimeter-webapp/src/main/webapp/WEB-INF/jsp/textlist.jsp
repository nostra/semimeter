<%@ page contentType="text/plain; charset=UTF-8" pageEncoding="UTF-8" session="false" trimDirectiveWhitespaces="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${empty numberOfItems}">0</c:if>
<c:if test="${! empty numberOfItems}">${numberOfItems}</c:if>
