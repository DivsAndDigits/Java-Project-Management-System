<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="common/header.jsp" />

<div class="main-wrapper" style="margin-left: 0;">
    <main class="content-container" style="display:flex; justify-content:center; align-items:center; min-height:80vh;">
        <div class="card" style="max-width: 550px; width: 100%; text-align: center; padding: 32px 24px;">
            <div style="margin-bottom: 20px;">
                <c:choose>
                    <c:when test="${errorCode == 403}">
                        <div class="avatar-initials avatar-lg" style="margin: 0 auto; background: #fee2e2; color: #dc2626; font-size: 2rem;">
                            🔒
                        </div>
                    </c:when>
                    <c:when test="${errorCode == 404}">
                        <div class="avatar-initials avatar-lg" style="margin: 0 auto; background: #fef3c7; color: #d97706; font-size: 2rem;">
                            🔍
                        </div>
                    </c:when>
                    <c:otherwise>
                        <div class="avatar-initials avatar-lg" style="margin: 0 auto; background: #e0e7ff; color: #4f46e5; font-size: 2rem;">
                            ⚠️
                        </div>
                    </c:otherwise>
                </c:choose>
            </div>

            <h1 style="font-size: 1.5rem; font-weight: 700; color: var(--text-main); margin-bottom: 12px;">
                <c:choose>
                    <c:when test="${errorCode == 403}">Access Denied</c:when>
                    <c:when test="${errorCode == 404}">Resource Not Found</c:when>
                    <c:otherwise>Application Notice</c:otherwise>
                </c:choose>
            </h1>

            <p style="color: var(--text-muted); font-size: 0.95rem; margin-bottom: 24px;">
                <c:out value="${errorMessage != null ? errorMessage : 'You do not have permission to view or modify this resource.'}" />
            </p>

            <div style="display: flex; justify-content: center; gap: 12px;">
                <a href="${pageContext.request.contextPath}/dashboard" class="btn btn-primary">
                    Return to Dashboard
                </a>
                <a href="javascript:history.back()" class="btn btn-secondary">
                    Go Back
                </a>
            </div>
        </div>
    </main>
</div>

<jsp:include page="common/footer.jsp" />
