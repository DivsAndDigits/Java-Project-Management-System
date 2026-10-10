<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<header class="top-navbar">
    <div class="navbar-left">
        <button type="button" class="sidebar-toggle-btn" id="sidebarToggleBtn" aria-label="Toggle Sidebar">
            <svg width="22" height="22" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5"/>
            </svg>
        </button>

        <div class="search-box">
            <span class="search-icon">
                <svg width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"/>
                </svg>
            </span>
            <input type="text" class="search-input" id="tableSearchInput" placeholder="Quick search projects, tasks..." />
        </div>
    </div>

    <div class="navbar-right">
        <div class="nav-actions">
            <!-- Create Project Action Button (Admin and Project Manager only) -->
            <c:if test="${sessionScope.currentUser != null && (sessionScope.currentUser.role == 'ADMIN' || sessionScope.currentUser.role == 'PROJECT_MANAGER')}">
                <button type="button" class="btn btn-primary btn-sm" data-open-modal="projectModal">
                    <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15"/>
                    </svg>
                    <span>New Project</span>
                </button>
            </c:if>

            <!-- Notifications Icon Button -->
            <a href="${pageContext.request.contextPath}/notifications" class="icon-btn" title="View Notifications">
                <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75v-.7V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"/>
                </svg>
                <c:if test="${unreadNotificationCount > 0}">
                    <span class="icon-badge">${unreadNotificationCount}</span>
                </c:if>
            </a>

            <!-- User Profile Dropdown -->
            <div class="user-profile-menu">
                <button type="button" class="user-profile-trigger" data-toggle="dropdown" data-target="userDropdown">
                    <div class="avatar-initials avatar-sm">${sessionScope.currentUser != null ? sessionScope.currentUser.avatarInitials : 'AM'}</div>
                    <svg width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" d="M19.5 8.25l-7.5 7.5-7.5-7.5"/>
                    </svg>
                </button>
                <div class="dropdown-menu" id="userDropdown">
                    <div style="padding: 10px 12px; border-bottom: 1px solid var(--border-color);">
                        <div style="font-weight:700; color:var(--text-main); font-size:0.9rem;">${sessionScope.currentUser != null ? sessionScope.currentUser.fullName : 'Alex Morgan'}</div>
                        <div style="font-size:0.75rem; color:var(--text-muted);">${sessionScope.currentUser != null ? sessionScope.currentUser.email : 'alex@company.com'}</div>
                    </div>
                    <a href="${pageContext.request.contextPath}/profile" class="dropdown-item">
                        <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"/>
                        </svg>
                        <span>My Profile</span>
                    </a>
                    <a href="${pageContext.request.contextPath}/tasks" class="dropdown-item">
                        <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
                        </svg>
                        <span>Assigned Tasks</span>
                    </a>
                    <c:choose>
                        <c:when test="${sessionScope.currentUser != null}">
                            <div class="dropdown-divider"></div>
                            <a href="${pageContext.request.contextPath}/logout" class="dropdown-item" style="color:var(--danger);">
                                <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"/>
                                </svg>
                                <span>Sign Out</span>
                            </a>
                        </c:when>
                        <c:otherwise>
                            <div class="dropdown-divider"></div>
                            <a href="${pageContext.request.contextPath}/login" class="dropdown-item" style="color:var(--primary);">
                                <svg width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"/>
                                </svg>
                                <span>Sign In</span>
                            </a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>
    </div>
</header>
