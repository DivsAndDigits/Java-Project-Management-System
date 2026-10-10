<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="common/navbar.jsp" />

    <main class="content-container">
        <!-- Optional Notification Banners -->
        <c:if test="${param.success != null}">
            <div class="alert alert-success">
                <span>✓ <c:out value="${param.success}" /></span>
                <button type="button" style="background:none;border:none;cursor:pointer;color:inherit;" onclick="this.parentElement.remove()">✕</button>
            </div>
        </c:if>
        <c:if test="${param.loggedOut != null}">
            <div class="alert alert-success">
                <span>✓ You have been safely logged out. Welcome back to guest view!</span>
                <button type="button" style="background:none;border:none;cursor:pointer;color:inherit;" onclick="this.parentElement.remove()">✕</button>
            </div>
        </c:if>

        <!-- Page Header -->
        <div class="page-header">
            <div>
                <h1 class="page-title">Executive Dashboard</h1>
                <p class="page-subtitle">Real-time status overview across project milestones, sprint velocity, and team delivery.</p>
            </div>
            <div class="header-actions">
                <button type="button" class="btn btn-secondary" data-open-modal="taskModal">
                    <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                    </svg>
                    <span>New Task</span>
                </button>
                <button type="button" class="btn btn-primary" data-open-modal="projectModal">
                    <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                    </svg>
                    <span>New Project</span>
                </button>
            </div>
        </div>

        <!-- Dashboard KPI Metric Cards -->
        <!-- Dashboard KPI Metric Cards (7 Core Database Metrics) -->
        <div class="kpi-grid" style="grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));">
            <!-- Metric 1: Total Projects -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Total Projects</span>
                    <div class="kpi-icon-wrapper icon-indigo">
                        <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 7.125C2.25 6.504 2.754 6 3.375 6h6c.621 0 1.125.504 1.125 1.125v4.5c0 .621-.504 1.125-1.125 1.125h-6A1.125 1.125 0 012.25 11.625v-4.5zM13.5 7.125c0-.621.504-1.125 1.125-1.125h6c.621 0 1.125.504 1.125 1.125v14.25c0 .621-.504 1.125-1.125 1.125h-6a1.125 1.125 0 01-1.125-1.125V7.125z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.totalProjects}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-neutral">Registered</span>
                    <span>Portfolio size</span>
                </div>
            </div>

            <!-- Metric 2: Active Projects -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Active Projects</span>
                    <div class="kpi-icon-wrapper icon-indigo">
                        <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 13.5l10.5-11.25L12 10.5h8.25L9.75 21.75 12 13.5H3.75z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.activeProjects}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-up">In Progress</span>
                    <span>Active delivery</span>
                </div>
            </div>

            <!-- Metric 3: Completed Projects -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Completed Projects</span>
                    <div class="kpi-icon-wrapper icon-emerald">
                        <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.completedProjects}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-up">Closed</span>
                    <span>Delivered</span>
                </div>
            </div>

            <!-- Metric 4: Pending Tasks -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Pending Tasks</span>
                    <div class="kpi-icon-wrapper icon-sky">
                        <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v6h4.5m4.5 0a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.pendingTasks}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-neutral">${stats.inProgressTasks} In Progress</span>
                    <span>of ${stats.totalTasks} total</span>
                </div>
            </div>

            <!-- Metric 5: Completed Tasks -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Completed Tasks</span>
                    <div class="kpi-icon-wrapper icon-emerald">
                        <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M4.5 12.75l6 6 9-13.5"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.completedTasks}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-up">Resolved</span>
                    <span>Sprint velocity</span>
                </div>
            </div>

            <!-- Metric 6: Overdue Tasks -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Overdue Tasks</span>
                    <div class="kpi-icon-wrapper icon-rose">
                        <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.overdueTasks}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-down">Action Req</span>
                    <span>Needs attention</span>
                </div>
            </div>

            <!-- Metric 7: Team Members Count -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Team Members</span>
                    <div class="kpi-icon-wrapper icon-emerald">
                        <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.teamMembersCount}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-up">Active</span>
                    <span>Across departments</span>
                </div>
            </div>
        </div>

        <!-- 2-Column Main Dashboard Section -->
        <div class="content-grid-2-1">
            <!-- Left Wide Column: Tables -->
            <div style="display:flex; flex-direction:column; gap:24px;">
                <!-- Recent & Priority Projects Table -->
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h2 class="card-title">
                                <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" style="color:var(--primary);">
                                    <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 12h16.5m-16.5 3.75h16.5M3.75 19.5h16.5M5.625 4.5h12.75a1.875 1.875 0 010 3.75H5.625a1.875 1.875 0 010-3.75z"/>
                                </svg>
                                <span>Recent Projects</span>
                            </h2>
                            <p class="card-subtitle">Active initiatives monitored for sprint deliverables and budget</p>
                        </div>
                        <a href="${pageContext.request.contextPath}/projects" class="btn btn-outline btn-sm">View All</a>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Project Details</th>
                                        <th>Status</th>
                                        <th>Progress</th>
                                        <th>Target Date</th>
                                        <th>Manager</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="proj" items="${stats.recentProjects}">
                                        <tr>
                                            <td>
                                                <a href="${pageContext.request.contextPath}/project-details?id=${proj.id}" class="project-cell-name">${proj.name}</a>
                                                <span class="project-cell-code">${proj.code}</span>
                                            </td>
                                            <td>
                                                <span class="badge ${proj.status.badgeClass}">
                                                    <span class="badge-dot"></span>
                                                    ${proj.status.displayName}
                                                </span>
                                            </td>
                                            <td style="min-width: 140px;">
                                                <div class="progress-label-group">
                                                    <span>${proj.completedTasks}/${proj.totalTasks} Tasks</span>
                                                    <span>${proj.progressPercentage}%</span>
                                                </div>
                                                <div class="progress-container">
                                                    <div class="progress-bar ${proj.progressPercentage > 70 ? 'progress-bar-emerald' : 'progress-bar-indigo'}" style="width: ${proj.progressPercentage}%;"></div>
                                                </div>
                                            </td>
                                            <td style="white-space: nowrap; font-size: 0.88rem; color: var(--text-muted);">
                                                ${proj.endDate}
                                            </td>
                                            <td>
                                                <div style="display:flex; align-items:center; gap:8px;">
                                                    <div class="avatar-initials avatar-sm" style="background:#4338ca;">${proj.manager != null ? proj.manager.avatarInitials : 'PM'}</div>
                                                    <span style="font-size:0.86rem; font-weight:500;">${proj.manager != null ? proj.manager.fullName : 'Lead'}</span>
                                                </div>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <!-- Priority Work Items Table -->
                <div class="card">
                    <div class="card-header">
                        <div>
                            <h2 class="card-title">
                                <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" style="color:var(--warning);">
                                    <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/>
                                </svg>
                                <span>High Priority Tasks</span>
                            </h2>
                            <p class="card-subtitle">Urgent milestones requiring active developer attention</p>
                        </div>
                        <a href="${pageContext.request.contextPath}/tasks" class="btn btn-outline btn-sm">View Tasks</a>
                    </div>
                    <div class="card-body p-0">
                        <div class="table-responsive">
                            <table class="data-table">
                                <thead>
                                    <tr>
                                        <th>Task</th>
                                        <th>Project</th>
                                        <th>Priority</th>
                                        <th>Assignee</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <c:forEach var="task" items="${stats.priorityTasks}">
                                        <tr>
                                            <td>
                                                <span style="font-weight:600; display:block;">${task.title}</span>
                                                <span style="font-size:0.78rem; color:var(--text-light);">Est: ${task.estimatedHours}h</span>
                                            </td>
                                            <td style="font-size:0.88rem; color:var(--text-muted);">${task.projectName}</td>
                                            <td>
                                                <span class="priority-badge ${task.priority.badgeClass}">
                                                    ${task.priority.displayName}
                                                </span>
                                            </td>
                                            <td>
                                                <div style="display:flex; align-items:center; gap:8px;">
                                                    <div class="avatar-initials avatar-sm" style="background:#0284c7;">${task.assignee != null ? task.assignee.avatarInitials : 'NA'}</div>
                                                    <span style="font-size:0.86rem;">${task.assignee != null ? task.assignee.fullName : 'Unassigned'}</span>
                                                </div>
                                            </td>
                                            <td>
                                                <span class="badge ${task.status.badgeClass}">
                                                    <span class="badge-dot"></span>
                                                    ${task.status.displayName}
                                                </span>
                                            </td>
                                        </tr>
                                    </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- Right Column: Status Summary & Team Workload -->
            <div style="display:flex; flex-direction:column; gap:24px;">
                <!-- Project Progress Summary Card -->
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Overall Progress</h3>
                    </div>
                    <div class="card-body">
                        <div style="text-align:center; padding: 12px 0 20px 0;">
                            <div style="font-size: 3rem; font-weight: 800; color: var(--primary); line-height: 1;">
                                ${stats.overallProgressPercentage}%
                            </div>
                            <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                                Cumulative Milestone Completion
                            </div>
                        </div>

                        <div style="display:flex; flex-direction:column; gap:16px;">
                            <div>
                                <div class="progress-label-group">
                                    <span>Active Sprints</span>
                                    <span>${stats.activeProjects} projects</span>
                                </div>
                                <div class="progress-container">
                                    <div class="progress-bar progress-bar-indigo" style="width: 70%;"></div>
                                </div>
                            </div>

                            <div>
                                <div class="progress-label-group">
                                    <span>Completed Milestones</span>
                                    <span>${stats.completedProjects} projects</span>
                                </div>
                                <div class="progress-container">
                                    <div class="progress-bar progress-bar-emerald" style="width: 40%;"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- [Collections: Map Integration] Tasks Grouped by Status -->
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Tasks by Status</h3>
                        <span class="badge badge-primary" style="font-size:0.75rem;">Map&lt;TaskStatus, Long&gt;</span>
                    </div>
                    <div class="card-body">
                        <div style="display:flex; flex-direction:column; gap:10px;">
                            <c:forEach var="entry" items="${stats.tasksByStatus}">
                                <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.86rem; padding: 4px 0; border-bottom: 1px dashed var(--border-color);">
                                    <span style="font-weight:600; color:var(--text-main);">${entry.key.displayName}</span>
                                    <span class="badge ${entry.key.badgeClass}">${entry.value}</span>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>

                <!-- Team Workload Distribution -->
                <div class="card">
                    <div class="card-header">
                        <h3 class="card-title">Team Allocation</h3>
                        <a href="${pageContext.request.contextPath}/team" class="btn btn-outline btn-sm">Team</a>
                    </div>
                    <div class="card-body">
                        <div style="display:flex; flex-direction:column; gap:18px;">
                            <c:forEach var="member" items="${stats.teamSummary}" end="3">
                                <div>
                                    <div style="display:flex; align-items:center; justify-content:space-between; margin-bottom:6px;">
                                        <div style="display:flex; align-items:center; gap:8px;">
                                            <div class="avatar-initials avatar-sm">${member.user.avatarInitials}</div>
                                            <div>
                                                <div style="font-size:0.88rem; font-weight:600;">${member.user.fullName}</div>
                                                <div style="font-size:0.75rem; color:var(--text-light);">${member.user.department}</div>
                                            </div>
                                        </div>
                                        <span style="font-size:0.8rem; font-weight:700; color:var(--primary);">${member.workloadPercentage}%</span>
                                    </div>
                                    <div class="progress-container">
                                        <div class="progress-bar ${member.workloadPercentage > 80 ? 'progress-bar-amber' : 'progress-bar-indigo'}" style="width: ${member.workloadPercentage}%;"></div>
                                    </div>
                                </div>
                            </c:forEach>
                        </div>
                    </div>
                </div>

                <!-- Quick Action Shortcuts -->
                <div class="card" style="background: linear-gradient(135deg, #4f46e5 0%, #3730a3 100%); color: #ffffff;">
                    <div class="card-body">
                        <h4 style="font-size: 1.15rem; font-weight: 700; margin-bottom: 8px;">Architecture Ready</h4>
                        <p style="font-size: 0.86rem; color: #c7d2fe; line-height: 1.5; margin-bottom: 16px;">
                            Structured with clean Jakarta Servlets, JSP tags, Service Layer, and DAO contracts ready for JDBC persistence.
                        </p>
                        <a href="${pageContext.request.contextPath}/reports" class="btn btn-secondary btn-sm" style="background:#ffffff; color:#3730a3; font-weight:700;">
                            View Analytical Reports →
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </main>

<jsp:include page="common/footer.jsp" />
