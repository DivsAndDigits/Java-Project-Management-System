<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="common/navbar.jsp" />

    <main class="content-container">
        <c:if test="${param.success != null}">
            <div class="alert alert-success">
                <span>✓ <c:out value="${param.success}" /></span>
                <button type="button" style="background:none;border:none;cursor:pointer;color:inherit;" onclick="this.parentElement.remove()">✕</button>
            </div>
        </c:if>

        <div class="page-header">
            <div>
                <h1 class="page-title">User Profile &amp; Settings</h1>
                <p class="page-subtitle">Manage personal account details, role permissions, and contact preferences.</p>
            </div>
        </div>

        <div class="content-grid-2-1">
            <!-- Left: Profile Edit Form -->
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title">Profile Information</h3>
                </div>
                <div class="card-body">
                    <form action="${pageContext.request.contextPath}/profile" method="post">
                        <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
                        <div style="display:flex; align-items:center; gap:20px; margin-bottom:24px;">
                            <div class="avatar-initials" style="width:64px; height:64px; font-size:1.5rem;">
                                ${user.avatarInitials}
                            </div>
                            <div>
                                <h3 style="font-size:1.2rem; font-weight:700;">${user.fullName}</h3>
                                <p style="color:var(--text-muted); font-size:0.85rem;">@${user.username} • <span class="badge badge-primary">${user.role.displayName}</span></p>
                            </div>
                        </div>

                        <div class="form-row" style="margin-bottom:16px;">
                            <div class="form-group">
                                <label class="form-label" for="username">Username (Read-only)</label>
                                <input type="text" id="username" class="form-input" value="${user.username}" disabled style="background:#f1f5f9; cursor:not-allowed;" />
                            </div>
                            <div class="form-group">
                                <label class="form-label" for="role">Role Permission</label>
                                <input type="text" id="role" class="form-input" value="${user.role.displayName}" disabled style="background:#f1f5f9; cursor:not-allowed;" />
                            </div>
                        </div>

                        <div class="form-group" style="margin-bottom:16px;">
                            <label class="form-label" for="fullName">Full Name</label>
                            <input type="text" id="fullName" name="fullName" class="form-input" value="${user.fullName}" required />
                        </div>

                        <div class="form-group" style="margin-bottom:16px;">
                            <label class="form-label" for="email">Work Email</label>
                            <input type="email" id="email" name="email" class="form-input" value="${user.email}" required />
                        </div>

                        <div class="form-group" style="margin-bottom:24px;">
                            <label class="form-label" for="department">Department</label>
                            <input type="text" id="department" name="department" class="form-input" value="${user.department}" required />
                        </div>

                        <div style="display:flex; justify-content:flex-end; gap:12px;">
                            <button type="submit" class="btn btn-primary">Save Changes</button>
                        </div>
                    </form>
                </div>
            </div>

            <!-- Right: Role Description & Assigned Tasks -->
            <div style="display:flex; flex-direction:column; gap:24px;">
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Permissions &amp; Scope</h3>
                    </div>
                    <div class="card-body">
                        <div style="font-weight:600; color:var(--text-main); margin-bottom:4px;">
                            ${user.roleTitle}
                        </div>
                        <p style="font-size:0.86rem; color:var(--text-muted); line-height:1.5;">
                            ${user.permissionsSummary}
                        </p>
                        <div style="margin-top:16px; padding:12px; background-color:#f8fafc; border-radius:var(--border-radius); font-size:0.82rem; color:var(--text-muted);">
                            <c:choose>
                                <c:when test="${user.administrativePrivileges}">
                                    ✓ Full System Administration &amp; Security Auditing<br>
                                    ✓ User Account Management &amp; Provisioning<br>
                                    ✓ Global System Settings &amp; Reporting Control
                                </c:when>
                                <c:when test="${user.canManageProjects()}">
                                    ✓ Project Creation &amp; Milestone Scoping<br>
                                    ✓ Sprint Task Assignment &amp; Workload Allocation<br>
                                    ✓ Executive Budget Authorization
                                </c:when>
                                <c:otherwise>
                                    ✓ Task Work Item Execution &amp; Updates<br>
                                    ✓ Hours Logging &amp; Cycle Time Tracking<br>
                                    ✓ Team Collaboration &amp; Pull Request Linkage
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">My Current Tasks</h3>
                    </div>
                    <div class="card-body">
                        <c:choose>
                            <c:when test="${not empty assignedTasks}">
                                <div style="display:flex; flex-direction:column; gap:12px;">
                                    <c:forEach var="task" items="${assignedTasks}">
                                        <div style="padding:10px; border:1px solid var(--border-color); border-radius:var(--border-radius-sm);">
                                            <div style="font-size:0.88rem; font-weight:600;">${task.title}</div>
                                            <div style="font-size:0.78rem; color:var(--text-muted); margin-top:2px;">Due: ${task.dueDate} • <span class="badge ${task.status.badgeClass}">${task.status.displayName}</span></div>
                                        </div>
                                    </c:forEach>
                                </div>
                            </c:when>
                            <c:otherwise>
                                <p style="font-size:0.85rem; color:var(--text-muted);">No urgent tasks assigned at the moment.</p>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>
        </div>
    </main>

<jsp:include page="common/footer.jsp" />
