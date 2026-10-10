/**
 * Online Project Management Tool - Core JavaScript Interactions
 */

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    initModals();
    initDropdowns();
    initFilters();
    initToastAlerts();
});

/* ==========================================================================
   Sidebar Management (Mobile Drawer & Collapse)
   ========================================================================== */
function initSidebar() {
    const sidebar = document.getElementById('appSidebar');
    const toggleBtn = document.getElementById('sidebarToggleBtn');

    if (!sidebar || !toggleBtn) return;

    toggleBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        sidebar.classList.toggle('open');
    });

    // Close when clicking outside of the sidebar on mobile
    document.addEventListener('click', (e) => {
        if (sidebar.classList.contains('open') && !sidebar.contains(e.target) && !toggleBtn.contains(e.target)) {
            sidebar.classList.remove('open');
        }
    });
}

/* ==========================================================================
   Modal Dialog Management
   ========================================================================== */
function initModals() {
    // Open triggers
    document.querySelectorAll('[data-open-modal]').forEach(trigger => {
        trigger.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = trigger.getAttribute('data-open-modal');
            openModal(targetId);
        });
    });

    // Close triggers
    document.querySelectorAll('[data-close-modal]').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.preventDefault();
            const modal = btn.closest('.modal-backdrop');
            if (modal) {
                closeModal(modal.id);
            }
        });
    });

    // Backdrop click to close
    document.querySelectorAll('.modal-backdrop').forEach(modal => {
        modal.addEventListener('click', (e) => {
            if (e.target === modal) {
                closeModal(modal.id);
            }
        });
    });

    // Escape key listener
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document.querySelectorAll('.modal-backdrop.show').forEach(modal => {
                closeModal(modal.id);
            });
            closeAllDropdowns();
        }
    });
}

function openModal(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    modal.classList.add('show');
    document.body.style.overflow = 'hidden';

    // Focus first input
    const firstInput = modal.querySelector('input, select, textarea');
    if (firstInput) {
        setTimeout(() => firstInput.focus(), 100);
    }
}

function closeModal(modalId) {
    const modal = document.getElementById(modalId);
    if (!modal) return;
    modal.classList.remove('show');
    document.body.style.overflow = '';
}

/* ==========================================================================
   Dropdown Menus
   ========================================================================== */
function initDropdowns() {
    document.querySelectorAll('[data-toggle="dropdown"]').forEach(trigger => {
        trigger.addEventListener('click', (e) => {
            e.stopPropagation();
            const targetId = trigger.getAttribute('data-target');
            const menu = document.getElementById(targetId);

            if (menu) {
                const isOpen = menu.classList.contains('show');
                closeAllDropdowns();
                if (!isOpen) {
                    menu.classList.add('show');
                }
            }
        });
    });

    // Close on outside click
    document.addEventListener('click', () => {
        closeAllDropdowns();
    });
}

function closeAllDropdowns() {
    document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
        menu.classList.remove('show');
    });
}

/* ==========================================================================
   Table Filtering (Interactive Client-side)
   ========================================================================== */
function initFilters() {
    const searchInput = document.getElementById('tableSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase().trim();
            const rows = document.querySelectorAll('.data-table tbody tr');

            rows.forEach(row => {
                const text = row.innerText.toLowerCase();
                row.style.display = text.includes(query) ? '' : 'none';
            });
        });
    }
}

/* ==========================================================================
   Toast Notifications
   ========================================================================== */
function initToastAlerts() {
    // Auto-dismiss static alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.4s ease';
            alert.style.opacity = '0';
            setTimeout(() => alert.remove(), 400);
        }, 5000);
    });
}

function showToast(message, type = 'info', duration = 4000) {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
        <div class="toast-content" style="flex:1;">
            <div style="font-weight:600; font-size:0.9rem;">${message}</div>
        </div>
        <button type="button" style="background:none; border:none; cursor:pointer; color:#94a3b8;" onclick="this.parentElement.remove()">✕</button>
    `;

    container.appendChild(toast);
    setTimeout(() => toast.classList.add('show'), 10);

    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

/* ==========================================================================
   Edit Modal Helper Functions
   ========================================================================== */
function openEditProjectModal(id, name, code, priority, status, budget, endDate, desc) {
    if (document.getElementById('modalEditProjId')) document.getElementById('modalEditProjId').value = id || '';
    if (document.getElementById('modalEditProjName')) document.getElementById('modalEditProjName').value = name || '';
    if (document.getElementById('modalEditProjCode')) document.getElementById('modalEditProjCode').value = code || '';
    if (document.getElementById('modalEditProjPriority')) document.getElementById('modalEditProjPriority').value = priority || 'MEDIUM';
    if (document.getElementById('modalEditProjStatus')) document.getElementById('modalEditProjStatus').value = status || 'PLANNING';
    if (document.getElementById('modalEditProjBudget')) document.getElementById('modalEditProjBudget').value = budget || '';
    if (document.getElementById('modalEditProjEndDate')) document.getElementById('modalEditProjEndDate').value = endDate || '';
    if (document.getElementById('modalEditProjDesc')) document.getElementById('modalEditProjDesc').value = desc || '';
    openModal('editProjectModal');
}

function openEditTaskModal(id, title, projectId, assigneeId, priority, status, dueDate, estHours, loggedHours, desc) {
    if (document.getElementById('modalEditTaskId')) document.getElementById('modalEditTaskId').value = id || '';
    if (document.getElementById('modalEditTaskTitle')) document.getElementById('modalEditTaskTitle').value = title || '';
    if (document.getElementById('modalEditTaskProject')) document.getElementById('modalEditTaskProject').value = projectId || '';
    if (document.getElementById('modalEditTaskAssignee')) document.getElementById('modalEditTaskAssignee').value = assigneeId || '';
    if (document.getElementById('modalEditTaskPriority')) document.getElementById('modalEditTaskPriority').value = priority || 'MEDIUM';
    if (document.getElementById('modalEditTaskStatus')) document.getElementById('modalEditTaskStatus').value = status || 'TODO';
    if (document.getElementById('modalEditTaskDueDate')) document.getElementById('modalEditTaskDueDate').value = dueDate || '';
    if (document.getElementById('modalEditTaskHours')) document.getElementById('modalEditTaskHours').value = estHours || 8;
    if (document.getElementById('modalEditTaskLoggedHours')) document.getElementById('modalEditTaskLoggedHours').value = loggedHours || 0;
    if (document.getElementById('modalEditTaskDesc')) document.getElementById('modalEditTaskDesc').value = desc || '';
    openModal('editTaskModal');
}

