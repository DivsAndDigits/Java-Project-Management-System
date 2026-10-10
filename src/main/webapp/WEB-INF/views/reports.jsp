<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="common/navbar.jsp" />

    <main class="content-container">
        <div class="page-header">
            <div>
                <h1 class="page-title">Executive Reports &amp; Analytics</h1>
                <p class="page-subtitle">Real-time JDBC data, Java collections aggregations, team workload, and delivery metrics.</p>
            </div>
        </div>

        <!-- Reporting KPI Summary Cards -->
        <div class="kpi-grid">
            <!-- 1. Project Completion Percentage -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Project Completion</span>
                    <div class="kpi-icon-wrapper icon-sky">
                        <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 6a7.5 7.5 0 107.5 7.5h-7.5V6z"/>
                            <path stroke-linecap="round" stroke-linejoin="round" d="M13.5 10.5H21A7.5 7.5 0 0013.5 3v7.5z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.overallProgressPercentage}%</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-up">Portfolio Avg</span>
                    <span>Across ${stats.totalProjects} registered initiatives</span>
                </div>
            </div>

            <!-- 2. Completed vs Pending Tasks -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Completed vs Pending Tasks</span>
                    <div class="kpi-icon-wrapper icon-emerald">
                        <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">${stats.completedTasks} / ${stats.pendingTasks}</div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-up">
                        <fmt:formatNumber value="${stats.totalTasks > 0 ? (stats.completedTasks * 100.0 / stats.totalTasks) : 0}" pattern="#,##0"/>% Complete
                    </span>
                    <span>Total Tasks: ${stats.totalTasks}</span>
                </div>
            </div>

            <!-- 3. Overdue Tasks KPI -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Overdue Tasks</span>
                    <div class="kpi-icon-wrapper ${stats.overdueTasks > 0 ? 'icon-rose' : 'icon-emerald'}">
                        <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 9v3.75m9-.75a9 9 0 11-18 0 9 9 0 0118 0zm-9 3.75h.008v.008H12v-.008z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value" style="${stats.overdueTasks > 0 ? 'color: #dc2626;' : ''}">${stats.overdueTasks}</div>
                <div class="kpi-footer">
                    <span class="trend-badge ${stats.overdueTasks > 0 ? 'trend-down' : 'trend-up'}">
                        ${stats.overdueTasks > 0 ? 'Action Needed' : 'On Track'}
                    </span>
                    <span>Deadline Approaching / Passed</span>
                </div>
            </div>

            <!-- 4. Total Budget & Burn Rate -->
            <div class="kpi-card">
                <div class="kpi-top">
                    <span class="kpi-title">Total Project Budget</span>
                    <div class="kpi-icon-wrapper icon-indigo">
                        <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 6v12m-3-2.818l.879.659c1.171.879 3.07.879 4.242 0 1.172-.879 1.172-2.303 0-3.182C13.536 12.219 12.768 12 12 12c-.725 0-1.45-.22-2.003-.659-1.106-.879-1.106-2.303 0-3.182s2.9-.879 4.006 0l.415.33M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                    </div>
                </div>
                <div class="kpi-value">$<fmt:formatNumber value="${stats.totalBudget}" pattern="#,##0"/></div>
                <div class="kpi-footer">
                    <span class="trend-badge trend-neutral">
                        $<fmt:formatNumber value="${stats.totalSpent}" pattern="#,##0"/> Spent
                    </span>
                    <span>Remaining: $<fmt:formatNumber value="${stats.totalBudget - stats.totalSpent}" pattern="#,##0"/></span>
                </div>
            </div>
        </div>

        <!-- [Collections: Map & Generics Integration] Dynamic Category Distributions -->
        <div class="content-grid-1-1" style="margin-top: 24px; margin-bottom: 24px;">
            <!-- Tasks Grouped by Priority -->
            <div class="card">
                <div class="card-header">
                    <div>
                        <h3 class="card-title">Tasks Grouped by Priority</h3>
                        <p style="font-size:0.75rem; color:var(--text-muted); margin:0;">Aggregated via Java Map&lt;Priority, Long&gt;</p>
                    </div>
                    <span class="badge badge-primary" style="font-size:0.75rem;">Map&lt;Priority, Long&gt;</span>
                </div>
                <div class="card-body">
                    <div style="display:flex; flex-direction:column; gap:14px;">
                        <c:forEach var="entry" items="${stats.tasksByPriority}">
                            <div>
                                <div style="display:flex; justify-content:space-between; font-size:0.86rem; margin-bottom:4px;">
                                    <span class="priority-badge ${entry.key.badgeClass}">${entry.key.displayName}</span>
                                    <span style="font-weight:700;">${entry.value} Tasks</span>
                                </div>
                                <div class="progress-container">
                                    <div class="progress-bar ${entry.key == 'URGENT' ? 'progress-bar-rose' : (entry.key == 'HIGH' ? 'progress-bar-amber' : 'progress-bar-indigo')}"
                                         style="width: ${stats.totalTasks > 0 ? (entry.value * 100 / stats.totalTasks) : 0}%;"></div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>

            <!-- Tasks Grouped by Status -->
            <div class="card">
                <div class="card-header">
                    <div>
                        <h3 class="card-title">Tasks Grouped by Status</h3>
                        <p style="font-size:0.75rem; color:var(--text-muted); margin:0;">Aggregated via Java Map&lt;TaskStatus, Long&gt;</p>
                    </div>
                    <span class="badge badge-primary" style="font-size:0.75rem;">Map&lt;TaskStatus, Long&gt;</span>
                </div>
                <div class="card-body">
                    <div style="display:flex; flex-direction:column; gap:14px;">
                        <c:forEach var="entry" items="${stats.tasksByStatus}">
                            <div>
                                <div style="display:flex; justify-content:space-between; font-size:0.86rem; margin-bottom:4px;">
                                    <span class="badge ${entry.key.badgeClass}">${entry.key.displayName}</span>
                                    <span style="font-weight:700;">${entry.value} Tasks</span>
                                </div>
                                <div class="progress-container">
                                    <div class="progress-bar ${entry.key == 'COMPLETED' ? 'progress-bar-emerald' : (entry.key == 'IN_PROGRESS' ? 'progress-bar-indigo' : (entry.key == 'OVERDUE' ? 'progress-bar-rose' : 'progress-bar-amber'))}"
                                         style="width: ${stats.totalTasks > 0 ? (entry.value * 100 / stats.totalTasks) : 0}%;"></div>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </div>
            </div>
        </div>

        <!-- Team Workload Distribution -->
        <div class="card" style="margin-bottom: 24px;">
            <div class="card-header">
                <div>
                    <h3 class="card-title">Team Workload &amp; Capacity Analytics</h3>
                    <p style="font-size:0.78rem; color:var(--text-muted); margin:0;">Live aggregation of tasks and completion velocity across all engineering contributors.</p>
                </div>
                <span class="badge badge-success">${stats.teamSummary.size()} Members</span>
            </div>
            <div class="card-body" style="padding: 0;">
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Team Member</th>
                                <th>Role</th>
                                <th>Department</th>
                                <th>Assigned Tasks</th>
                                <th>Completed</th>
                                <th>Sprint Velocity</th>
                                <th>Workload Completion</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="member" items="${stats.teamSummary}">
                                <tr>
                                    <td>
                                        <div style="display:flex; align-items:center; gap:10px;">
                                            <div class="avatar-initials avatar-sm">${member.user.avatarInitials}</div>
                                            <div>
                                                <div style="font-weight:600; color:var(--text-main); font-size:0.88rem;">${member.user.fullName}</div>
                                                <div style="font-size:0.75rem; color:var(--text-muted);">${member.user.email}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <span class="badge badge-secondary" style="font-size:0.75rem;">${member.user.role.displayName}</span>
                                    </td>
                                    <td>${member.user.department}</td>
                                    <td><strong style="color:var(--text-main);">${member.assignedTasksCount}</strong></td>
                                    <td><span style="color:var(--primary); font-weight:600;">${member.completedTasksCount}</span></td>
                                    <td>${member.sprintVelocity} pts</td>
                                    <td style="min-width: 140px;">
                                        <c:set var="memberPercent" value="${member.assignedTasksCount > 0 ? (member.completedTasksCount * 100 / member.assignedTasksCount) : 0}" />
                                        <div style="display:flex; justify-content:space-between; font-size:0.78rem; margin-bottom:4px;">
                                            <span>${memberPercent}%</span>
                                        </div>
                                        <div class="progress-container">
                                            <div class="progress-bar ${memberPercent >= 80 ? 'progress-bar-emerald' : 'progress-bar-indigo'}"
                                                 style="width: ${memberPercent}%;"></div>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>

        <!-- Overdue Tasks Analysis -->
        <c:if test="${not empty overdueTaskList}">
            <div class="card" style="margin-bottom: 24px; border-left: 4px solid #ef4444;">
                <div class="card-header">
                    <div>
                        <h3 class="card-title" style="color: #dc2626;">Overdue Tasks Requiring Attention</h3>
                        <p style="font-size:0.78rem; color:var(--text-muted); margin:0;">Active tasks with passed target deadlines retrieved via TaskDao.findOverdueTasks().</p>
                    </div>
                    <span class="badge badge-danger">${overdueTaskList.size()} Overdue</span>
                </div>
                <div class="card-body" style="padding: 0;">
                    <div class="table-responsive">
                        <table class="table">
                            <thead>
                                <tr>
                                    <th>Task Title</th>
                                    <th>Project</th>
                                    <th>Assignee</th>
                                    <th>Priority</th>
                                    <th>Due Date</th>
                                    <th>Status</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="ot" items="${overdueTaskList}">
                                    <tr>
                                        <td style="font-weight:600; color:var(--text-main);">${ot.title}</td>
                                        <td>${ot.projectName != null ? ot.projectName : 'Project #'.concat(ot.projectId)}</td>
                                        <td>${ot.assignee != null ? ot.assignee.fullName : 'Unassigned'}</td>
                                        <td><span class="priority-badge ${ot.priority.badgeClass}">${ot.priority.displayName}</span></td>
                                        <td style="color:#dc2626; font-weight:600;">${ot.dueDate}</td>
                                        <td><span class="badge badge-danger">Overdue</span></td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- Polymorphic Analytical Reports Section -->
        <c:if test="${not empty reports}">
            <div style="margin-top: 8px;">
                <h2 style="font-size: 1.3rem; font-weight: 700; color: var(--text-main); margin-bottom: 16px;">
                    Automated Analytical Findings
                </h2>
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px;">
                    <c:forEach var="report" items="${reports}">
                        <div class="card">
                            <div class="card-header">
                                <div>
                                    <h3 class="card-title" style="font-size: 1rem;">${report.title}</h3>
                                    <div style="font-size: 0.76rem; color: var(--text-light);">Generated: ${report.generatedAt}</div>
                                </div>
                                <span class="badge ${report.healthBadge == 'HEALTHY' ? 'badge-success' : (report.healthBadge == 'CRITICAL' ? 'badge-danger' : 'badge-warning')}">
                                    <span class="badge-dot"></span>
                                    ${report.healthBadge}
                                </span>
                            </div>
                            <div class="card-body">
                                <p style="font-size: 0.85rem; color: var(--text-muted); margin-bottom: 16px;">
                                    ${report.description}
                                </p>
                                
                                <!-- Metrics Grid -->
                                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; background: #f8fafc; padding: 12px; border-radius: var(--border-radius); margin-bottom: 16px;">
                                    <c:forEach var="entry" items="${report.summaryMetrics}">
                                        <div>
                                            <div style="font-size: 0.74rem; color: var(--text-muted); text-transform: uppercase;">${entry.key}</div>
                                            <div style="font-size: 0.95rem; font-weight: 700; color: var(--text-main);">${entry.value}</div>
                                        </div>
                                    </c:forEach>
                                </div>

                                <!-- Key Observations -->
                                <div style="font-size: 0.82rem; color: var(--text-main);">
                                    <strong style="color: var(--text-muted); display: block; margin-bottom: 6px;">Key Observations:</strong>
                                    <ul style="padding-left: 18px; margin: 0; display: flex; flex-direction: column; gap: 4px;">
                                        <c:forEach var="obs" items="${report.keyObservations}">
                                            <li>${obs}</li>
                                        </c:forEach>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </c:forEach>
                </div>
            </div>
        </c:if>
    </main>

<jsp:include page="common/footer.jsp" />
