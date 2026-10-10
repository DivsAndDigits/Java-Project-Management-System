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
        <c:if test="${param.error != null}">
            <div class="alert alert-danger">
                <span>⚠ <c:out value="${param.error}" /></span>
                <button type="button" style="background:none;border:none;cursor:pointer;color:inherit;" onclick="this.parentElement.remove()">✕</button>
            </div>
        </c:if>

        <div class="page-header">
            <div>
                <h1 class="page-title">Team Directory &amp; Capacity</h1>
                <p class="page-subtitle">Track resource allocation, cross-functional engineering teams, and sprint capacity.</p>
            </div>
        </div>

        <!-- Team Members Grid -->
        <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 24px;">
            <c:forEach var="item" items="${teamSummaries}">
                <div class="card">
                    <div class="card-body">
                        <div style="display:flex; align-items:flex-start; justify-content:space-between; margin-bottom:16px;">
                            <div style="display:flex; align-items:center; gap:14px;">
                                <div class="avatar-initials" style="width:48px; height:48px; font-size:1.1rem;">
                                    ${item.user.avatarInitials}
                                </div>
                                <div>
                                    <div style="font-size:1.05rem; font-weight:700; color:var(--text-main);">${item.user.fullName}</div>
                                    <div style="font-size:0.82rem; color:var(--text-muted);">${item.user.email}</div>
                                    <div style="margin-top:4px;">
                                        <span class="badge badge-primary">${item.user.roleTitle}</span>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <!-- Stats Row -->
                        <div style="display:grid; grid-template-columns: 1fr 1fr 1fr; gap:10px; background-color:#f8fafc; border-radius:var(--border-radius); padding:12px; margin-bottom:16px; text-align:center;">
                            <div>
                                <div style="font-size:1.15rem; font-weight:700; color:var(--text-main);">${item.activeProjects}</div>
                                <div style="font-size:0.72rem; color:var(--text-muted); text-transform:uppercase;">Projects</div>
                            </div>
                            <div>
                                <div style="font-size:1.15rem; font-weight:700; color:var(--text-main);">${item.assignedTasks}</div>
                                <div style="font-size:0.72rem; color:var(--text-muted); text-transform:uppercase;">Assigned</div>
                            </div>
                            <div>
                                <div style="font-size:1.15rem; font-weight:700; color:var(--success);">${item.completedTasks}</div>
                                <div style="font-size:0.72rem; color:var(--text-muted); text-transform:uppercase;">Closed</div>
                            </div>
                        </div>

                        <!-- Workload Progress Bar -->
                        <div>
                            <div class="progress-label-group">
                                <span style="font-size:0.8rem; color:var(--text-muted);">Sprint Capacity Utilization</span>
                                <span style="font-size:0.8rem; font-weight:700; color:${item.workloadPercentage > 85 ? 'var(--danger)' : 'var(--primary)'};">${item.workloadPercentage}%</span>
                            </div>
                            <div class="progress-container">
                                <div class="progress-bar ${item.workloadPercentage > 85 ? 'progress-bar-amber' : 'progress-bar-indigo'}" style="width: ${item.workloadPercentage}%;"></div>
                            </div>
                        </div>

                        <div style="margin-top:14px; font-size:0.82rem; color:var(--text-muted);">
                            <strong>Focus:</strong> ${item.currentFocus} • <span style="color:var(--text-light);">${item.user.department}</span>
                        </div>
                    </div>
                </div>
            </c:forEach>
        </div>
        <c:if test="${empty teamSummaries}">
            <div class="card" style="text-align:center; padding:48px 24px; color:var(--text-muted); margin-top:16px;">
                <p style="font-size:1.1rem; font-weight:600; margin-bottom:8px;">No team members registered yet.</p>
                <p style="font-size:0.875rem;">Team members and engineering workloads will appear here once registered.</p>
            </div>
        </c:if>
    </main>

<jsp:include page="common/footer.jsp" />
