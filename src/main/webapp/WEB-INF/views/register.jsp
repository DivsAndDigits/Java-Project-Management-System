<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="common/header.jsp" />

<div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background: var(--bg-body); padding: 24px;">
    <div class="card" style="width: 100%; max-width: 520px; padding: 36px 32px; box-shadow: var(--shadow-lg); border-radius: var(--radius-lg);">
        <div style="text-align: center; margin-bottom: 24px;">
            <div style="width: 52px; height: 52px; background: linear-gradient(135deg, var(--primary), var(--primary-dark)); border-radius: 12px; display: inline-flex; align-items: center; justify-content: center; color: white; margin-bottom: 16px;">
                <svg width="28" height="28" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M19 7.5v3m0 0v3m0-3h3m-3 0h-3m-2.25-4.125a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zM4 19.235v-.11a6.375 6.375 0 0112.75 0v.109A12.318 12.318 0 0110.374 21c-2.331 0-4.512-.645-6.374-1.765z"/>
                </svg>
            </div>
            <h2 style="font-size: 1.5rem; font-weight: 700; color: var(--text-main); margin-bottom: 6px;">Create an Account</h2>
            <p style="font-size: 0.875rem; color: var(--text-muted);">Join your team on ProjectFlow</p>
        </div>

        <c:if test="${param.error != null}">
            <div class="alert alert-danger" style="margin-bottom: 20px;">
                <span>⚠️ <c:out value="${param.error}" /></span>
            </div>
        </c:if>

        <form action="${pageContext.request.contextPath}/register" method="POST">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px;">
                <div class="form-group">
                    <label class="form-label" for="username">Username *</label>
                    <input type="text" id="username" name="username" class="form-control" placeholder="johndoe" required />
                </div>
                <div class="form-group">
                    <label class="form-label" for="fullName">Full Name *</label>
                    <input type="text" id="fullName" name="fullName" class="form-control" placeholder="John Doe" required />
                </div>
            </div>

            <div class="form-group" style="margin-bottom: 16px;">
                <label class="form-label" for="email">Email Address *</label>
                <input type="email" id="email" name="email" class="form-control" placeholder="john.doe@company.com" required />
            </div>

            <div class="form-group" style="margin-bottom: 16px;">
                <label class="form-label" for="password">Password *</label>
                <input type="password" id="password" name="password" class="form-control" placeholder="Minimum 6 characters" required />
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 24px;">
                <div class="form-group">
                    <label class="form-label" for="role">Role</label>
                    <select id="role" name="role" class="form-control">
                        <option value="TEAM_MEMBER">Team Member</option>
                        <option value="PROJECT_MANAGER">Project Manager</option>
                        <option value="ADMIN">Administrator</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="department">Department</label>
                    <input type="text" id="department" name="department" class="form-control" placeholder="Engineering" />
                </div>
            </div>

            <button type="submit" class="btn btn-primary" style="width: 100%; justify-content: center; padding: 12px; font-size: 0.95rem;">
                Create Account
            </button>
        </form>

        <div style="margin-top: 24px; text-align: center; font-size: 0.85rem; color: var(--text-muted);">
            Already have an account? 
            <a href="${pageContext.request.contextPath}/login" style="color: var(--primary); font-weight: 600; text-decoration: none;">Sign in</a>
        </div>

        <div style="margin-top: 16px; text-align: center; font-size: 0.8rem; color: var(--text-light);">
            <a href="${pageContext.request.contextPath}/dashboard" style="color: var(--text-muted); text-decoration: none;">← Return to Dashboard</a>
        </div>
    </div>
</div>

<jsp:include page="common/footer.jsp" />
