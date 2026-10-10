<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="common/navbar.jsp" />

    <main class="content-container">
        <!-- Notification Banners -->
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

        <!-- Breadcrumb Navigation -->
        <div style="display:flex; align-items:center; gap:8px; margin-bottom:16px; font-size:0.86rem; color:var(--text-muted);">
            <a href="${pageContext.request.contextPath}/dashboard" style="color:var(--text-muted); text-decoration:none;">Dashboard</a>
            <span>/</span>
            <a href="${pageContext.request.contextPath}/projects" style="color:var(--text-muted); text-decoration:none;">Projects</a>
            <span>/</span>
            <span style="color:var(--primary); font-weight:600;">${project.name}</span>
        </div>

        <!-- Project Hero Card -->
        <div class="card" style="margin-bottom:24px; border-left:4px solid var(--primary); padding:24px;">
            <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:16px;">
                <div>
                    <div style="display:flex; align-items:center; gap:8px; margin-bottom:8px;">
                        <span class="badge ${project.status.badgeClass}">
                            <span class="badge-dot"></span>
                            ${project.status.displayName}
                        </span>
                        <span class="priority-badge ${project.priority.badgeClass}">
                            ${project.priority.displayName} Priority
                        </span>
                        <span class="badge" style="background:var(--bg-hover); color:var(--text-muted); font-family:monospace;">
                            ${project.code}
                        </span>
                    </div>
                    <h1 style="font-size:1.6rem; font-weight:700; color:var(--text-main); margin:0 0 6px 0;">${project.name}</h1>
                    <p style="color:var(--text-muted); font-size:0.92rem; margin:0; max-width:750px;">${project.description}</p>
                </div>
                <c:if test="${canManageProject}">
                    <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
                        <button type="button" class="btn btn-outline btn-sm" data-open-modal="editProjectModal">
                            <svg width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L6.832 19.82a4.5 4.5 0 01-1.897 1.13l-2.685.8.8-2.685a4.5 4.5 0 011.13-1.897L16.863 4.487zm0 0L19.5 7.125"/>
                            </svg>
                            <span>Edit Project</span>
                        </button>
                        <button type="button" class="btn btn-primary btn-sm" data-open-modal="addProjectTaskModal">
                            <svg width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                            </svg>
                            <span>Add Task</span>
                        </button>
                    </div>
                </c:if>
            </div>

            <!-- Key Metadata & Progress Grid -->
            <div style="display:grid; grid-template-columns:repeat(auto-fit, minmax(170px, 1fr)); gap:18px; margin-top:20px; padding-top:20px; border-top:1px solid var(--border-color);">
                <div>
                    <span style="font-size:0.75rem; color:var(--text-light); text-transform:uppercase; font-weight:600;">Project Lead</span>
                    <div style="display:flex; align-items:center; gap:8px; margin-top:4px;">
                        <div class="avatar-initials avatar-sm" style="background:#4338ca;">${project.manager != null ? project.manager.avatarInitials : 'PM'}</div>
                        <span style="font-weight:600; color:var(--text-main); font-size:0.88rem;">${project.manager != null ? project.manager.fullName : 'Unassigned'}</span>
                    </div>
                </div>

                <div>
                    <span style="font-size:0.75rem; color:var(--text-light); text-transform:uppercase; font-weight:600;">Budget / Spent</span>
                    <div style="font-weight:700; color:var(--text-main); font-size:0.95rem; margin-top:4px;">
                        $<fmt:formatNumber value="${project.budget}" pattern="#,##0"/>
                        <span style="font-size:0.78rem; font-weight:400; color:var(--text-light);"> / $<fmt:formatNumber value="${project.spentAmount}" pattern="#,##0"/></span>
                    </div>
                </div>

                <div>
                    <span style="font-size:0.75rem; color:var(--text-light); text-transform:uppercase; font-weight:600;">Schedule Timeline</span>
                    <div style="font-weight:600; color:var(--text-main); font-size:0.88rem; margin-top:4px;">
                        ${project.startDate} → ${project.endDate}
                    </div>
                </div>

                <div>
                    <span style="font-size:0.75rem; color:var(--text-light); text-transform:uppercase; font-weight:600;">Task Metrics</span>
                    <div style="font-weight:700; color:var(--text-main); font-size:0.95rem; margin-top:4px;">
                        ${completedTasks} / ${totalTasks}
                        <span style="font-size:0.78rem; font-weight:400; color:var(--text-muted);">Completed</span>
                    </div>
                </div>
            </div>

            <!-- Dynamic Project Progress Bar -->
            <div style="margin-top:20px;">
                <div class="progress-label-group">
                    <span style="font-weight:600; font-size:0.88rem;">Project Completion Rate</span>
                    <span style="font-weight:700; font-size:0.95rem; color:${progressPercentage == 100 ? 'var(--success)' : 'var(--primary)'};">${progressPercentage}%</span>
                </div>
                <div class="progress-container" style="height:10px;">
                    <div class="progress-bar ${progressPercentage >= 100 ? 'progress-bar-emerald' : 'progress-bar-indigo'}" style="width:${progressPercentage}%;"></div>
                </div>
            </div>
        </div>

        <!-- 2-Column Section: Left (Tasks), Right (Team) -->
        <div style="display:grid; grid-template-columns: 2.2fr 1fr; gap:24px; align-items:start;">
            
            <!-- Left Column: Tasks Table -->
            <div class="card">
                <div class="card-header" style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <h2 class="card-title" style="display:flex; align-items:center; gap:8px;">
                            <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24" style="color:var(--primary);">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                            </svg>
                            <span>Project Tasks (${totalTasks})</span>
                        </h2>
                        <p class="card-subtitle">Work items, milestones, and deliverables assigned to this initiative</p>
                    </div>
                    <div style="display:flex; gap:6px;">
                        <span class="badge badge-success" style="font-size:0.72rem;">${completedTasks} Closed</span>
                        <c:if test="${overdueTasks > 0}">
                            <span class="badge badge-danger" style="font-size:0.72rem;">${overdueTasks} Overdue</span>
                        </c:if>
                    </div>
                </div>

                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="data-table">
                            <thead>
                                <tr>
                                    <th>Task</th>
                                    <th>Status</th>
                                    <th>Priority</th>
                                    <th>Assignee</th>
                                    <th>Hours</th>
                                    <th>Due Date</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:choose>
                                    <c:when test="${not empty tasks}">
                                        <c:forEach var="task" items="${tasks}">
                                            <tr>
                                                <td>
                                                    <span style="font-weight:600; color:var(--text-main); display:block;">${task.title}</span>
                                                    <span style="font-size:0.78rem; color:var(--text-light);">${task.description}</span>
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
                                                    <div style="display:flex; align-items:center; gap:6px;">
                                                        <div class="avatar-initials avatar-sm" style="background:#0284c7; width:26px; height:26px; font-size:0.7rem;">
                                                            ${task.assignee != null ? task.assignee.avatarInitials : 'NA'}
                                                        </div>
                                                        <span style="font-size:0.84rem;">${task.assignee != null ? task.assignee.fullName : 'Unassigned'}</span>
                                                    </div>
                                                </td>
                                                <td>
                                                    <span style="font-weight:600; font-size:0.84rem;">${task.loggedHours}h</span>
                                                    <span style="font-size:0.75rem; color:var(--text-light);"> / ${task.estimatedHours}h</span>
                                                </td>
                                                <td style="white-space:nowrap; font-size:0.84rem; ${task.overdue ? 'color:var(--danger); font-weight:700;' : 'color:var(--text-muted);'}">
                                                    ${task.dueDate} ${task.overdue ? '⚠' : ''}
                                                </td>
                                                <td style="white-space:nowrap;">
                                                    <div style="display:flex; gap:6px; align-items:center;">
                                                        <c:if test="${canManageProject || (task.assignee != null && (task.assignee.id == sessionScope.currentUser.id || task.assignee.id == sessionScope.user.id))}">
                                                            <c:choose>
                                                                <c:when test="${task.status != 'COMPLETED'}">
                                                                    <a href="${pageContext.request.contextPath}/project-details?id=${project.id}&action=taskStatus&taskId=${task.id}&newStatus=COMPLETED"
                                                                       class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem; color:var(--success);" title="Mark Completed">✓ Done</a>
                                                                </c:when>
                                                                <c:otherwise>
                                                                    <a href="${pageContext.request.contextPath}/project-details?id=${project.id}&action=taskStatus&taskId=${task.id}&newStatus=IN_PROGRESS"
                                                                       class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem; color:var(--primary);" title="Reopen Task">↻ Reopen</a>
                                                                </c:otherwise>
                                                            </c:choose>
                                                        </c:if>
                                                        <c:if test="${canManageProject}">
                                                            <a href="${pageContext.request.contextPath}/project-details?id=${project.id}&action=deleteTask&taskId=${task.id}"
                                                               class="btn btn-outline btn-sm" style="padding:3px 7px; font-size:0.72rem; color:var(--danger);"
                                                               onclick="return confirm('Delete task: ${task.title}?');" title="Delete Task">✕</a>
                                                        </c:if>
                                                    </div>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                    </c:when>
                                    <c:otherwise>
                                        <tr>
                                            <td colspan="7" style="text-align:center; padding:32px; color:var(--text-muted);">
                                                No tasks created for this project yet. Click <strong>Add Task</strong> above to assign the first deliverable.
                                            </td>
                                        </tr>
                                    </c:otherwise>
                                </c:choose>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- Right Column: Project Team Roster -->
            <div class="card">
                <div class="card-header" style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <h2 class="card-title">Assigned Team</h2>
                        <p class="card-subtitle">Project contributors (${teamMembers.size()})</p>
                    </div>
                </div>
                <div class="card-body">
                    <!-- Add Member Form -->
                    <c:if test="${canManageProject}">
                        <form action="${pageContext.request.contextPath}/project-details" method="post" style="display:flex; gap:6px; margin-bottom:16px;">
                            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
                            <input type="hidden" name="action" value="addMember" />
                            <input type="hidden" name="id" value="${project.id}" />
                            <select name="userId" class="form-select" style="font-size:0.82rem; padding:6px 8px;" required>
                                <option value="">Select team member...</option>
                                <c:forEach var="u" items="${allUsers}">
                                    <option value="${u.id}">${u.fullName} (${u.role.displayName})</option>
                                </c:forEach>
                            </select>
                            <button type="submit" class="btn btn-secondary btn-sm" style="padding:6px 12px; white-space:nowrap;">+ Add</button>
                        </form>
                    </c:if>

                    <!-- Members List -->
                    <div style="display:flex; flex-direction:column; gap:12px;">
                        <c:choose>
                            <c:when test="${not empty teamMembers}">
                                <c:forEach var="member" items="${teamMembers}">
                                    <div style="display:flex; align-items:center; justify-content:space-between; padding:8px 10px; background:#f8fafc; border-radius:8px; border:1px solid var(--border-color);">
                                        <div style="display:flex; align-items:center; gap:10px;">
                                            <div class="avatar-initials avatar-sm" style="width:34px; height:34px; font-size:0.8rem; background:#0284c7;">
                                                ${member.avatarInitials}
                                            </div>
                                            <div>
                                                <div style="font-weight:600; font-size:0.86rem; color:var(--text-main);">${member.fullName}</div>
                                                <div style="font-size:0.75rem; color:var(--text-muted);">${member.roleTitle} • ${member.department}</div>
                                            </div>
                                        </div>
                                        <c:if test="${canManageProject}">
                                            <div>
                                                <a href="${pageContext.request.contextPath}/project-details?id=${project.id}&action=removeMember&userId=${member.id}"
                                                   style="color:var(--danger); text-decoration:none; font-size:0.85rem; padding:4px;"
                                                   onclick="return confirm('Remove ${member.fullName} from project?');" title="Remove member">✕</a>
                                            </div>
                                        </c:if>
                                    </div>
                                </c:forEach>
                            </c:when>
                            <c:otherwise>
                                <p style="font-size:0.85rem; color:var(--text-muted); text-align:center; margin:16px 0;">No team members assigned yet.</p>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

        </div>
    </main>

<!-- ==========================================================================
     Edit Project Modal Dialog
     ========================================================================== -->
<div class="modal-backdrop" id="editProjectModal" role="dialog" aria-modal="true" aria-labelledby="editProjectModalTitle">
    <div class="modal-dialog">
        <form action="${pageContext.request.contextPath}/project-details" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <input type="hidden" name="action" value="updateProject" />
            <input type="hidden" name="id" value="${project.id}" />

            <div class="modal-header">
                <h3 class="modal-title" id="editProjectModalTitle">Edit Project: ${project.name}</h3>
                <button type="button" class="modal-close-btn" data-close-modal aria-label="Close modal">
                    <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                </button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="editProjName">Project Name *</label>
                    <input type="text" id="editProjName" name="name" value="${project.name}" class="form-input" required />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="editProjCode">Project Code</label>
                        <input type="text" id="editProjCode" name="code" value="${project.code}" class="form-input" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="editProjPriority">Priority</label>
                        <select id="editProjPriority" name="priority" class="form-select">
                            <option value="LOW" ${project.priority == 'LOW' ? 'selected' : ''}>Low</option>
                            <option value="MEDIUM" ${project.priority == 'MEDIUM' ? 'selected' : ''}>Medium</option>
                            <option value="HIGH" ${project.priority == 'HIGH' ? 'selected' : ''}>High</option>
                            <option value="URGENT" ${project.priority == 'URGENT' ? 'selected' : ''}>Urgent</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="editProjStatus">Status</label>
                    <select id="editProjStatus" name="status" class="form-select">
                        <option value="PLANNING" ${project.status == 'PLANNING' ? 'selected' : ''}>Planning</option>
                        <option value="IN_PROGRESS" ${project.status == 'IN_PROGRESS' ? 'selected' : ''}>In Progress</option>
                        <option value="ON_HOLD" ${project.status == 'ON_HOLD' ? 'selected' : ''}>On Hold</option>
                        <option value="COMPLETED" ${project.status == 'COMPLETED' ? 'selected' : ''}>Completed</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="editProjDesc">Description</label>
                    <textarea id="editProjDesc" name="description" class="form-textarea" rows="3">${project.description}</textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="editProjBudget">Estimated Budget ($)</label>
                        <input type="number" id="editProjBudget" name="budget" value="${project.budget}" class="form-input" min="0" step="500" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="editProjEndDate">Target Deadline</label>
                        <input type="date" id="editProjEndDate" name="endDate" value="${project.endDate}" class="form-input" />
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
                <button type="submit" class="btn btn-primary">Save Changes</button>
            </div>
        </form>
    </div>
</div>

<!-- ==========================================================================
     Add Task to Project Modal Dialog
     ========================================================================== -->
<div class="modal-backdrop" id="addProjectTaskModal" role="dialog" aria-modal="true" aria-labelledby="addProjectTaskTitle">
    <div class="modal-dialog">
        <form action="${pageContext.request.contextPath}/project-details" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <input type="hidden" name="action" value="addTask" />
            <input type="hidden" name="id" value="${project.id}" />

            <div class="modal-header">
                <h3 class="modal-title" id="addProjectTaskTitle">Add Task to ${project.name}</h3>
                <button type="button" class="modal-close-btn" data-close-modal aria-label="Close modal">
                    <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                </button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="pTaskTitle">Task Title *</label>
                    <input type="text" id="pTaskTitle" name="title" class="form-input" placeholder="e.g. Implement REST API Endpoints" required />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="pTaskPriority">Priority</label>
                        <select id="pTaskPriority" name="priority" class="form-select">
                            <option value="LOW">Low</option>
                            <option value="MEDIUM" selected>Medium</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="pTaskHours">Estimated Hours</label>
                        <input type="number" id="pTaskHours" name="estimatedHours" class="form-input" value="8" min="1" />
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="pTaskDesc">Description</label>
                    <textarea id="pTaskDesc" name="description" class="form-textarea" rows="2" placeholder="Task deliverables..."></textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="pTaskAssignee">Assignee</label>
                        <select id="pTaskAssignee" name="assigneeId" class="form-select">
                            <option value="">Unassigned</option>
                            <c:forEach var="u" items="${allUsers}">
                                <option value="${u.id}">${u.fullName} (${u.role.displayName})</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="pTaskDueDate">Due Date</label>
                        <input type="date" id="pTaskDueDate" name="dueDate" class="form-input" />
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
                <button type="submit" class="btn btn-primary">Create Task</button>
            </div>
        </form>
    </div>
</div>

<jsp:include page="common/footer.jsp" />
