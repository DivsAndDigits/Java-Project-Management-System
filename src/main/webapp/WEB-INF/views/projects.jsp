<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="common/navbar.jsp" />

    <main class="content-container">
        <!-- Optional Alert messages -->
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
                <h1 class="page-title">Project Portfolio</h1>
                <p class="page-subtitle">Track, monitor, and coordinate enterprise projects and delivery roadmap.</p>
            </div>
            <div class="header-actions">
                <button type="button" class="btn btn-primary" data-open-modal="projectModal">
                    <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                    </svg>
                    <span>Create Project</span>
                </button>
            </div>
        </div>

        <!-- Filter, Sort & Search Toolbar (Combined Multi-Criteria) -->
        <div class="filter-bar" style="margin-bottom:20px; background:#f8fafc; border:1px solid var(--border-color); border-radius:var(--border-radius); padding:14px;">
            <form action="${pageContext.request.contextPath}/projects" method="get" style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
                <input type="text" name="search" value="${searchQuery}" placeholder="Search name, code, description..." class="form-control" style="width:220px; padding:6px 12px; font-size:0.86rem;" />

                <select name="status" class="form-select" style="width:140px; padding:6px 10px; font-size:0.84rem;">
                    <option value="ALL" ${selectedStatus == 'ALL' ? 'selected' : ''}>All Statuses</option>
                    <c:forEach var="st" items="${projectStatuses}">
                        <option value="${st.name()}" ${selectedStatus == st.name() ? 'selected' : ''}>${st.displayName}</option>
                    </c:forEach>
                </select>

                <select name="priority" class="form-select" style="width:130px; padding:6px 10px; font-size:0.84rem;">
                    <option value="ALL" ${selectedPriority == 'ALL' ? 'selected' : ''}>All Priorities</option>
                    <c:forEach var="pr" items="${priorities}">
                        <option value="${pr.name()}" ${selectedPriority == pr.name() ? 'selected' : ''}>${pr.displayName}</option>
                    </c:forEach>
                </select>

                <select name="sort" class="form-select" style="width:140px; padding:6px 10px; font-size:0.84rem;">
                    <option value="" ${empty selectedSort ? 'selected' : ''}>Default Sort</option>
                    <option value="progress" ${selectedSort == 'progress' ? 'selected' : ''}>Progress</option>
                    <option value="priority" ${selectedSort == 'priority' ? 'selected' : ''}>Priority</option>
                    <option value="startDate" ${selectedSort == 'startDate' ? 'selected' : ''}>Start Date</option>
                    <option value="endDate" ${selectedSort == 'endDate' ? 'selected' : ''}>Deadline</option>
                    <option value="name" ${selectedSort == 'name' ? 'selected' : ''}>Name</option>
                </select>

                <button type="submit" class="btn btn-secondary btn-sm" style="padding:6px 14px;">Apply Filters</button>
                <c:if test="${not empty searchQuery or selectedStatus != 'ALL' or selectedPriority != 'ALL' or not empty selectedSort}">
                    <a href="${pageContext.request.contextPath}/projects" class="btn btn-outline btn-sm" style="padding:6px 10px;">Reset</a>
                </c:if>
            </form>
        </div>

        <c:if test="${selectedProject != null}">
            <div class="card" style="margin-bottom: 24px; border-left: 4px solid var(--primary); padding: 20px;">
                <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 12px;">
                    <div>
                        <span class="badge ${selectedProject.status.badgeClass}">${selectedProject.status.displayName}</span>
                        <span class="priority-badge ${selectedProject.priority.badgeClass}" style="margin-left: 6px;">${selectedProject.priority.displayName}</span>
                        <h2 style="font-size: 1.3rem; margin: 8px 0 4px 0; color: var(--text-main); font-weight: 700;">${selectedProject.name} (${selectedProject.code})</h2>
                        <p style="color: var(--text-muted); font-size: 0.88rem; margin: 0;">${selectedProject.description}</p>
                    </div>
                    <a href="${pageContext.request.contextPath}/projects" class="btn btn-secondary btn-sm">Close View</a>
                </div>
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 16px; margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-color);">
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Lead / Manager</span>
                        <div style="font-weight: 600; color: var(--text-main);">${selectedProject.manager != null ? selectedProject.manager.fullName : 'None'}</div>
                    </div>
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Budget / Spent</span>
                        <div style="font-weight: 600; color: var(--text-main);">$<fmt:formatNumber value="${selectedProject.budget}" pattern="#,##0"/> / $<fmt:formatNumber value="${selectedProject.spentAmount}" pattern="#,##0"/></div>
                    </div>
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Progress</span>
                        <div style="font-weight: 600; color: var(--text-main);">${selectedProject.progressPercentage}% (${selectedProject.completedTasks}/${selectedProject.totalTasks} tasks)</div>
                    </div>
                    <div>
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase;">Target Deadline</span>
                        <div style="font-weight: 600; color: var(--text-main);">${selectedProject.endDate}</div>
                    </div>
                </div>
                <c:if test="${not empty projectTeam}">
                    <div style="margin-top: 16px; padding-top: 12px; border-top: 1px dashed var(--border-color);">
                        <span style="font-size: 0.75rem; color: var(--text-light); text-transform: uppercase; display: block; margin-bottom: 8px;">Assigned Team Members:</span>
                        <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                            <c:forEach var="member" items="${projectTeam}">
                                <span class="badge" style="background: var(--bg-hover); color: var(--text-main); font-weight: 500; padding: 4px 10px; border-radius: 20px;">
                                    ${member.fullName} (${member.role.displayName})
                                </span>
                            </c:forEach>
                        </div>
                    </div>
                </c:if>
            </div>
        </c:if>

        <!-- Projects Table -->
        <div class="card">
            <div class="card-body p-0">
                <div class="table-responsive">
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Project Name</th>
                                <th>Status</th>
                                <th>Priority</th>
                                <th>Progress</th>
                                <th>Budget Allocation</th>
                                <th>Target Deadline</th>
                                <th>Lead & Team</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <c:forEach var="proj" items="${projects}">
                                <tr>
                                    <td>
                                        <a href="${pageContext.request.contextPath}/project-details?id=${proj.id}" class="project-cell-name">${proj.name}</a>
                                        <span class="project-cell-code">${proj.code} • ${proj.description}</span>
                                    </td>
                                    <td>
                                        <span class="badge ${proj.status.badgeClass}">
                                            <span class="badge-dot"></span>
                                            ${proj.status.displayName}
                                        </span>
                                    </td>
                                    <td>
                                        <span class="priority-badge ${proj.priority.badgeClass}">
                                            ${proj.priority.displayName}
                                        </span>
                                    </td>
                                    <td style="min-width: 140px;">
                                        <div class="progress-label-group">
                                            <span>${proj.completedTasks}/${proj.totalTasks} Done</span>
                                            <span>${proj.progressPercentage}%</span>
                                        </div>
                                        <div class="progress-container">
                                            <div class="progress-bar ${proj.progressPercentage > 75 ? 'progress-bar-emerald' : 'progress-bar-indigo'}" style="width: ${proj.progressPercentage}%;"></div>
                                        </div>
                                    </td>
                                    <td>
                                        <div style="font-weight: 600; font-size: 0.9rem;">
                                            $<fmt:formatNumber value="${proj.budget}" pattern="#,##0"/>
                                        </div>
                                        <span style="font-size: 0.78rem; color: var(--text-light);">Spent: $<fmt:formatNumber value="${proj.spentAmount}" pattern="#,##0"/></span>
                                    </td>
                                    <td style="white-space: nowrap; color: var(--text-muted);">
                                        ${proj.endDate}
                                    </td>
                                    <td>
                                        <div style="display:flex; align-items:center; gap:8px;">
                                            <div class="avatar-initials avatar-sm" title="Manager: ${proj.manager != null ? proj.manager.fullName : 'PM'}">
                                                ${proj.manager != null ? proj.manager.avatarInitials : 'PM'}
                                            </div>
                                            <div class="avatar-group">
                                                <c:forEach var="member" items="${proj.teamMembers}" end="2">
                                                    <div class="avatar-initials avatar-sm" style="background:#0284c7;" title="${member.fullName}">
                                                        ${member.avatarInitials}
                                                    </div>
                                                </c:forEach>
                                            </div>
                                        </div>
                                    </td>
                                    <td style="white-space: nowrap;">
                                        <div style="display:flex; gap:6px; align-items:center;">
                                            <a href="${pageContext.request.contextPath}/project-details?id=${proj.id}" class="btn btn-outline btn-sm" style="padding:4px 8px; font-size:0.75rem;" title="View Project Details">View</a>
                                            <c:if test="${sessionScope.currentUser != null && (sessionScope.currentUser.role == 'ADMIN' || (sessionScope.currentUser.role == 'PROJECT_MANAGER' && (proj.managerId == null || proj.managerId == sessionScope.currentUser.id)))}">
                                                <button type="button" class="btn btn-outline btn-sm" style="padding:4px 8px; font-size:0.75rem;" onclick="openEditProjectModal('${proj.id}', '${proj.name}', '${proj.code}', '${proj.priority.name()}', '${proj.status.name()}', '${proj.budget}', '${proj.endDate}', '${proj.description}')" title="Edit Project">Edit</button>
                                            </c:if>
                                            <c:if test="${sessionScope.currentUser != null && sessionScope.currentUser.role == 'ADMIN'}">
                                                <a href="${pageContext.request.contextPath}/projects?action=delete&id=${proj.id}" class="btn btn-outline btn-sm" style="padding:4px 8px; font-size:0.75rem; color:var(--danger);" onclick="return confirm('Are you sure you want to delete project: ${proj.name}?');" title="Delete Project">Delete</a>
                                            </c:if>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty projects}">
                                <tr>
                                    <td colspan="8" style="text-align:center; padding:32px 16px; color:var(--text-muted);">
                                        No projects found matching the criteria. Click "New Project" above to create a project initiative.
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
