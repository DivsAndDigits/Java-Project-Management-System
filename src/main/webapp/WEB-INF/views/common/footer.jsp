<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

    </div><!-- End of main-wrapper -->
</div><!-- End of app-container -->

<!-- ==========================================================================
     Create Project Modal Dialog
     ========================================================================== -->
<div class="modal-backdrop" id="projectModal" role="dialog" aria-modal="true" aria-labelledby="projectModalTitle">
    <div class="modal-dialog">
        <form action="${pageContext.request.contextPath}/projects" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <div class="modal-header">
                <h3 class="modal-title" id="projectModalTitle">Create New Project</h3>
                <button type="button" class="modal-close-btn" data-close-modal aria-label="Close modal">
                    <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                </button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="projName">Project Name *</label>
                    <input type="text" id="projName" name="name" class="form-input" placeholder="e.g. NextGen Microservices Platform" required />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="projCode">Project Code</label>
                        <input type="text" id="projCode" name="code" class="form-input" placeholder="e.g. PRJ-201" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="projPriority">Priority</label>
                        <select id="projPriority" name="priority" class="form-select">
                            <option value="LOW">Low</option>
                            <option value="MEDIUM" selected>Medium</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="projDesc">Description</label>
                    <textarea id="projDesc" name="description" class="form-textarea" rows="3" placeholder="Scope, key deliverables, and architectural milestones..."></textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="projBudget">Estimated Budget ($)</label>
                        <input type="number" id="projBudget" name="budget" class="form-input" placeholder="50000" min="0" step="500" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="projEndDate">Target Deadline</label>
                        <input type="date" id="projEndDate" name="endDate" class="form-input" />
                    </div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-close-modal>Cancel</button>
                <button type="submit" class="btn btn-primary">Create Project</button>
            </div>
        </form>
    </div>
</div>

<!-- ==========================================================================
     Create Task Modal Dialog
     ========================================================================== -->
<div class="modal-backdrop" id="taskModal" role="dialog" aria-modal="true" aria-labelledby="taskModalTitle">
    <div class="modal-dialog">
        <form action="${pageContext.request.contextPath}/tasks" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <div class="modal-header">
                <h3 class="modal-title" id="taskModalTitle">Create New Task</h3>
                <button type="button" class="modal-close-btn" data-close-modal aria-label="Close modal">
                    <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                </button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="taskTitle">Task Title *</label>
                    <input type="text" id="taskTitle" name="title" class="form-input" placeholder="e.g. Implement OAuth2 Refresh Token Rotation" required />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="taskProject">Assign to Project *</label>
                        <select id="taskProject" name="projectId" class="form-select">
                            <c:choose>
                                <c:when test="${not empty projects}">
                                    <c:forEach var="p" items="${projects}">
                                        <option value="${p.id}">${p.name}</option>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <option value="1">Default Project</option>
                                </c:otherwise>
                            </c:choose>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="taskPriority">Priority</label>
                        <select id="taskPriority" name="priority" class="form-select">
                            <option value="LOW">Low</option>
                            <option value="MEDIUM" selected>Medium</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="taskDesc">Description</label>
                    <textarea id="taskDesc" name="description" class="form-textarea" rows="3" placeholder="Technical specifications, criteria of done, acceptance tests..."></textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="taskAssignee">Assignee</label>
                        <select id="taskAssignee" name="assigneeId" class="form-select">
                            <option value="">Unassigned</option>
                            <c:forEach var="u" items="${users}">
                                <option value="${u.id}">${u.fullName} (${u.role.displayName})</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="taskDueDate">Due Date</label>
                        <input type="date" id="taskDueDate" name="dueDate" class="form-input" />
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

<!-- ==========================================================================
     Global Edit Project Modal Dialog
     ========================================================================== -->
<div class="modal-backdrop" id="editProjectModal" role="dialog" aria-modal="true" aria-labelledby="editProjectModalGlobalTitle">
    <div class="modal-dialog">
        <form action="${pageContext.request.contextPath}/projects" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <input type="hidden" name="action" value="update" />
            <input type="hidden" name="id" id="modalEditProjId" />

            <div class="modal-header">
                <h3 class="modal-title" id="editProjectModalGlobalTitle">Edit Project</h3>
                <button type="button" class="modal-close-btn" data-close-modal aria-label="Close modal">
                    <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                </button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="modalEditProjName">Project Name *</label>
                    <input type="text" id="modalEditProjName" name="name" class="form-input" required />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="modalEditProjCode">Project Code</label>
                        <input type="text" id="modalEditProjCode" name="code" class="form-input" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="modalEditProjPriority">Priority</label>
                        <select id="modalEditProjPriority" name="priority" class="form-select">
                            <option value="LOW">Low</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="modalEditProjStatus">Status</label>
                    <select id="modalEditProjStatus" name="status" class="form-select">
                        <option value="PLANNING">Planning</option>
                        <option value="IN_PROGRESS">In Progress</option>
                        <option value="ON_HOLD">On Hold</option>
                        <option value="COMPLETED">Completed</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label" for="modalEditProjDesc">Description</label>
                    <textarea id="modalEditProjDesc" name="description" class="form-textarea" rows="3"></textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="modalEditProjBudget">Estimated Budget ($)</label>
                        <input type="number" id="modalEditProjBudget" name="budget" class="form-input" min="0" step="500" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="modalEditProjEndDate">Target Deadline</label>
                        <input type="date" id="modalEditProjEndDate" name="endDate" class="form-input" />
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
     Global Edit Task Modal Dialog
     ========================================================================== -->
<div class="modal-backdrop" id="editTaskModal" role="dialog" aria-modal="true" aria-labelledby="editTaskModalGlobalTitle">
    <div class="modal-dialog">
        <form action="${pageContext.request.contextPath}/tasks" method="post">
            <input type="hidden" name="csrfToken" value="<c:out value='${sessionScope.csrfToken}' />" />
            <input type="hidden" name="action" value="update" />
            <input type="hidden" name="id" id="modalEditTaskId" />

            <div class="modal-header">
                <h3 class="modal-title" id="editTaskModalGlobalTitle">Edit Task</h3>
                <button type="button" class="modal-close-btn" data-close-modal aria-label="Close modal">
                    <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12"/>
                    </svg>
                </button>
            </div>
            <div class="modal-body">
                <div class="form-group">
                    <label class="form-label" for="modalEditTaskTitle">Task Title *</label>
                    <input type="text" id="modalEditTaskTitle" name="title" class="form-input" required />
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="modalEditTaskProject">Project *</label>
                        <select id="modalEditTaskProject" name="projectId" class="form-select">
                            <c:forEach var="p" items="${projects}">
                                <option value="${p.id}">${p.name}</option>
                            </c:forEach>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="modalEditTaskAssignee">Assignee</label>
                        <select id="modalEditTaskAssignee" name="assigneeId" class="form-select">
                            <option value="">Unassigned</option>
                            <c:forEach var="u" items="${users}">
                                <option value="${u.id}">${u.fullName}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="modalEditTaskPriority">Priority</label>
                        <select id="modalEditTaskPriority" name="priority" class="form-select">
                            <option value="LOW">Low</option>
                            <option value="MEDIUM">Medium</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                        </select>
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="modalEditTaskStatus">Status</label>
                        <select id="modalEditTaskStatus" name="status" class="form-select">
                            <option value="TODO">To Do</option>
                            <option value="IN_PROGRESS">In Progress</option>
                            <option value="IN_REVIEW">In Review</option>
                            <option value="COMPLETED">Completed</option>
                            <option value="OVERDUE">Overdue</option>
                            <option value="BLOCKED">Blocked</option>
                        </select>
                    </div>
                </div>
                <div class="form-group">
                    <label class="form-label" for="modalEditTaskDesc">Description</label>
                    <textarea id="modalEditTaskDesc" name="description" class="form-textarea" rows="2"></textarea>
                </div>
                <div class="form-row">
                    <div class="form-group">
                        <label class="form-label" for="modalEditTaskDueDate">Due Date</label>
                        <input type="date" id="modalEditTaskDueDate" name="dueDate" class="form-input" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="modalEditTaskHours">Est. Hours</label>
                        <input type="number" id="modalEditTaskHours" name="estimatedHours" class="form-input" min="0" />
                    </div>
                    <div class="form-group">
                        <label class="form-label" for="modalEditTaskLoggedHours">Logged Hours</label>
                        <input type="number" id="modalEditTaskLoggedHours" name="loggedHours" class="form-input" min="0" />
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

<!-- Toast Container for Notifications -->
<div id="toastContainer" class="toast-container"></div>

<!-- Application Main JavaScript -->
<script src="${pageContext.request.contextPath}/js/main.js"></script>
</body>
</html>
