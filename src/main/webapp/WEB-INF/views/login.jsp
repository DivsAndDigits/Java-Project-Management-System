<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="common/header.jsp" />

<div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-body); padding: 24px;">
    <div class="card" style="width: 100%; max-width: 440px; padding: 36px 32px; box-shadow: var(--shadow-lg); border-radius: var(--radius-lg);">
        <div style="text-align: center; margin-bottom: 28px;">
            <div style="width: 52px; height: 52px; background: linear-gradient(135deg, var(--primary), var(--primary-dark)); border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; color: white; margin-bottom: 16px;">
                <svg width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"/>
                </svg>
            </div>
            <h2 style="font-size: 1.5rem; font-weight: 700; color: var(--text-main); margin-bottom: 6px;">Sign in to ProjectFlow</h2>
            <p style="font-size: 0.875rem; color: var(--text-muted);">Enterprise Project & Sprint Management</p>
        </div>

        <c:if test="${param.loggedOut != null}">
            <div class="alert alert-success" style="margin-bottom: 20px;">
                <span>✓ You have been successfully signed out.</span>
            </div>
        </c:if>
        <c:if test="${param.error != null}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span>⚠️ <c:out value="${param.error}" /></span>
            </div>
        </c:if>
        <c:if test="${param.success != null}">
            <div class="alert alert-success" style="margin-bottom: 20px;">
                <span>✓ <c:out value="${param.success}" /></span>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/login" method="POST">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <div class="form-group" style="margin-bottom: 18px;">
                <label class="form-label" for="usernameOrEmail">Username or Email</label>
                <input type="text" id="usernameOrEmail" name="usernameOrEmail" class="form-control" 
                       placeholder="e.g. alex.morgan or alex.morgan@company.com" required autofocus />
            </div>

            <div class="form-group" style="margin-bottom: 24px;">
                <label class="form-label" for="password">Password</label>
                <input type="password" id="password" name="password" class="form-control" 
                       placeholder="Enter your account password" required />
            </div>

            <button type="submit" class="btn btn-primary" style="width: 100%; justify-content: center; padding: 12px; font-size: 0.95rem;">
                Sign In
            </button>
        </form>

        <div style="margin-top: 24px; text-align: center; font-size: 0.85rem; color: var(--text-muted);">
            Don't have an account? 
            <a href="${pageContext.request.contextPath}/register" style="color: var(--primary); font-weight: 600; text-decoration: none;">Register here</a>
        </div>

        <div style="margin-top: 16px; text-align: center; font-size: 0.8rem; color: var(--text-light);">
            <a href="${pageContext.request.contextPath}/dashboard" style="color: var(--text-muted); text-decoration: none;">← Return to Dashboard</a>
        </div>

        <div style="margin-top: 24px; padding: 14px 16px; background: rgba(99, 102, 241, 0.06); border-radius: 8px; border: 1px dashed var(--border-color); font-size: 0.8rem; color: var(--text-muted); line-height: 1.6;">
            <div style="font-weight: 600; color: var(--text-main); margin-bottom: 4px;">Default Demo Credentials:</div>
            <div>• Manager: <code style="color: var(--primary); font-weight: 600;">alex.morgan</code> / <code>password123</code></div>
            <div>• Admin: <code style="color: var(--primary); font-weight: 600;">sarah.chen</code> / <code>password123</code></div>
            <div>• Member: <code style="color: var(--primary); font-weight: 600;">elena.rostova</code> / <code>password123</code></div>
        </div>
    </div>
</div>

<jsp:include page="common/footer.jsp" />
