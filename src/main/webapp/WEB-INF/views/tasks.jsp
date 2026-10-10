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
                <h1 class="page-title">Tasks &amp; Sprint Work Items</h1>
                <p class="page-subtitle">Manage engineering work items, bug resolutions, and sprint tasks.</p>
            </div>
            <c:if test="${sessionScope.currentUser != null && (sessionScope.currentUser.role == 'ADMIN' || sessionScope.currentUser.role == 'PROJECT_MANAGER')}">
                <div class="header-actions">
                    <button type="button" class="btn btn-primary" data-open-modal="taskModal">
                        <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                        </svg>
                        <span>Create Task</span>
                    </button>
                </div>
            </c:if>
        </div>

        <!-- Filter, Sort & Search Toolbar (Combined Multi-Criteria) -->
        <div class="filter-bar" style="margin-bottom:20px; background:#f8fafc; border:1px solid var(--border-color); border-radius:var(--border-radius); padding:14px;">
            <form action="${pageContext.request.contextPath}/tasks" method="get" style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
                <input type="text" name="search" value="${searchQuery}" placeholder="Search task title, desc..." class="form-control" style="width:190px; padding:6px 12px; font-size:0.86rem;" />

                <select name="projectId" class="form-select" style="width:160px; padding:6px 10px; font-size:0.84rem;">
                    <option value="">All Projects</option>
                    <c:forEach var="p" items="${projects}">
                        <option value="${p.id}" ${selectedProjectId == p.id ? 'selected' : ''}>${p.name}</option>
                    </c:forEach>
                </select>

                <select name="status" class="form-select" style="width:130px; padding:6px 10px; font-size:0.84rem;">
                    <option value="ALL" ${selectedStatus == 'ALL' ? 'selected' : ''}>All Statuses</option>
                    <c:forEach var="st" items="${taskStatuses}">
                        <option value="${st.name()}" ${selectedStatus == st.name() ? 'selected' : ''}>${st.displayName}</option>
                    </c:forEach>
                </select>

                <select name="priority" class="form-select" style="width:120px; padding:6px 10px; font-size:0.84rem;">
                    <option value="ALL" ${selectedPriority == 'ALL' ? 'selected' : ''}>All Priorities</option>
                    <c:forEach var="pr" items="${priorities}">
                        <option value="${pr.name()}" ${selectedPriority == pr.name() ? 'selected' : ''}>${pr.displayName}</option>
                    </c:forEach>
                </select>

                <select name="sort" class="form-select" style="width:125px; padding:6px 10px; font-size:0.84rem;">
                    <option value="" ${empty selectedSort ? 'selected' : ''}>Default Sort</option>
                    <option value="deadline" ${selectedSort == 'deadline' ? 'selected' : ''}>Deadline</option>
                    <option value="priority" ${selectedSort == 'priority' ? 'selected' : ''}>Priority</option>
                    <option value="status" ${selectedSort == 'status' ? 'selected' : ''}>Status</option>
                    <option value="hours" ${selectedSort == 'hours' ? 'selected' : ''}>Hours</option>
                    <option value="title" ${selectedSort == 'title' ? 'selected' : ''}>Title</option>
                </select>

                <label style="display:flex; align-items:center; gap:5px; font-size:0.84rem; color:var(--danger); cursor:pointer;">
                    <input type="checkbox" name="overdue" value="true" ${param.overdue == 'true' ? 'checked' : ''} />
                    <span>Overdue Only</span>
                </label>

                <button type="submit" class="btn btn-secondary btn-sm" style="padding:6px 14px;">Apply</button>
                <c:if test="${not empty searchQuery or selectedStatus != 'ALL' or selectedPriority != 'ALL' or not empty selectedProjectId or not empty selectedSort or param.overdue == 'true'}">
                    <a href="${pageContext.request.contextPath}/tasks" class="btn btn-outline btn-sm" style="padding:6px 10px;">Reset</a>
                </c:if>
            </form>
        </div>

        <c:if test="${selectedTask != null}">
            <div class="card" style="margin-bottom: 24px; border-left: 4px solid var(--primary); padding: 20px;">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
                    <div>
                        <span class="badge ${selectedTask.status.badgeClass}">${selectedTask.status.displayName}</span>
                        <span class="priority-badge ${selectedTask.priority.badgeClass}" style="margin-left: 6px;">${selectedTask.priority.displayName}</span>
                        <h2 style="font-size: 1.3rem; margin: 8px 0 4px 0; color: var(--text-main); font-weight: 700;">${selectedTask.title}</h2>
                        <p style="color: var(--text-muted); font-size: 0.88rem; margin: 0;">${selectedTask.description}</p>
                    </div>
                    <a href="${pageContext.request.contextPath}/tasks" class="btn btn-secondary btn-sm">Close View</a>
                </div>
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-color);">
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Project</span>
                        <div style="font-weight: 600; color: var(--text-main);">${selectedTask.projectName}</div>
                    </div>
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Assignee</span>
                        <div style="font-weight: 600; color: var(--text-main);">${selectedTask.assignee != null ? selectedTask.assignee.fullName : 'Unassigned'}</div>
                    </div>
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Effort</span>
                        <div style="font-weight: 600; color: var(--text-main);">${selectedTask.loggedHours}h / ${selectedTask.estimatedHours}h</div>
                    </div>
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Due Date</span>
                        <div style="font-weight: 600; color: var(--text-main);">${selectedTask.dueDate}</div>
                    </div>
                </div>
            </div>
        </c:if>

        <!-- Tasks Table -->
        <div class="card">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Task Details</th>
                                <th>Associated Project</th>
                                <th>Status</th>
                                <th>Priority</th>
                                <th>Assignee</th>
                                <th>Hours (Logged/Est)</th>
                                <th>Due Date</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="task" items="${tasks}">
                                <tr>
                                    <td>
                                        <span style="font-weight: 600; color: var(--text-main); display:block;">${task.title}</span>
                                        <span style="font-size: 0.8rem; color: var(--text-light);">${task.description}</span>
                                    </td>
                                    <td>
                                        <span style="font-size: 0.88rem; font-weight: 500; color: var(--text-muted);">${task.projectName}</span>
                                    </td>
                                    <td>
                                        <span class="badge ${task.status.badgeClass}">
                                            <span class="badge-dot"></span>
                                            ${task.status.displayName}
                                        </span>
                                    </td>
                                    <td>
                                        <span class="priority-badge ${task.priority.badgeClass}">
                                            ${task.priority.displayName}
                                        </span>
                                    </td>
                                    <td>
                                        <div style="display:flex; align-items:center; gap:8px;">
                                            <div class="avatar-initials avatar-sm" style="background:#0284c7;">${task.assignee != null ? task.assignee.avatarInitials : 'NA'}</div>
                                            <span style="font-size:0.86rem; font-weight:500;">${task.assignee != null ? task.assignee.fullName : 'Unassigned'}</span>
                                        </div>
                                    </td>
                                    <td>
                                        <span style="font-weight: 600; font-size: 0.88rem;">${task.loggedHours}h</span>
                                        <span style="font-size: 0.78rem; color: var(--text-light);"> / ${task.estimatedHours}h</span>
                                    </td>
                                    <td style="white-space: nowrap; font-size: 0.88rem; ${task.overdue ? 'color: var(--danger); font-weight: 700;' : 'color: var(--text-muted);'}">
                                        ${task.dueDate} ${task.overdue ? '⚠ Overdue' : ''}
                                    </td>
                                    <td style="white-space: nowrap;">
                                        <div style="display:flex; gap:6px; align-items:center;">
                                            <a href="${pageContext.request.contextPath}/tasks?action=view&id=${task.id}" class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem;" title="View Details">View</a>
                                            <c:if test="${(sessionScope.currentUser != null && (sessionScope.currentUser.role == 'ADMIN' || sessionScope.currentUser.role == 'PROJECT_MANAGER')) || (sessionScope.user != null && (sessionScope.user.role == 'ADMIN' || sessionScope.user.role == 'PROJECT_MANAGER'))}">
                                                <button type="button" class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem;" onclick="openEditTaskModal('${task.id}', '${task.title}', '${task.projectId}', '${task.assignee != null ? task.assignee.id : ''}', '${task.priority.name()}', '${task.status.name()}', '${task.dueDate}', '${task.estimatedHours}', '${task.loggedHours}', '${task.description}')" title="Edit Task">Edit</button>
                                            </c:if>
                                            <c:if test="${(sessionScope.currentUser != null && (sessionScope.currentUser.role == 'ADMIN' || sessionScope.currentUser.role == 'PROJECT_MANAGER' || (task.assignee != null && task.assignee.id == sessionScope.currentUser.id))) || (sessionScope.user != null && (sessionScope.user.role == 'ADMIN' || sessionScope.user.role == 'PROJECT_MANAGER' || (task.assignee != null && task.assignee.id == sessionScope.user.id)))}">
                                                <c:choose>
                                                    <c:when test="${task.status != 'COMPLETED'}">
                                                        <a href="${pageContext.request.contextPath}/tasks?action=status&id=${task.id}&newStatus=COMPLETED" class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem; color:var(--success);" title="Mark Completed">✓ Done</a>
                                                    </c:when>
                                                    <c:otherwise>
                                                        <a href="${pageContext.request.contextPath}/tasks?action=status&id=${task.id}&newStatus=IN_PROGRESS" class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem; color:var(--primary);" title="Reopen Task">↻ Reopen</a>
                                                    </c:otherwise>
                                                </c:choose>
                                            </c:if>
                                            <c:if test="${(sessionScope.currentUser != null && (sessionScope.currentUser.role == 'ADMIN' || sessionScope.currentUser.role == 'PROJECT_MANAGER')) || (sessionScope.user != null && (sessionScope.user.role == 'ADMIN' || sessionScope.user.role == 'PROJECT_MANAGER'))}">
                                                <a href="${pageContext.request.contextPath}/tasks?action=delete&id=${task.id}" class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem; color:var(--danger);" onclick="return confirm('Delete task: ${task.title}?');" title="Delete Task">✕</a>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty tasks}">
                                <tr>
                                    <td colspan="8" style="text-align:center; padding:32px 16px; color:var(--text-muted);">
                                        No tasks found matching the criteria. Click "Create Task" above to add new work items.
                                    </td>
                                </tr>
                            </c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>

<jsp:include page="common/footer.jsp" />
