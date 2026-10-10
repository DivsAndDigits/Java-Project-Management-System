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
                <h1 class="page-title">Notifications Center</h1>
                <p class="page-subtitle">Stay notified of code reviews, task assignments, sprint changes, and system alerts.</p>
            </div>
            <div class="header-actions">
                <a href="${pageContext.request.contextPath}/notifications?action=checkDeadlines" class="btn btn-outline btn-sm" title="Trigger background deadline scan">
                    ⚡ Check Deadlines
                </a>
                <c:if test="${unreadCount > 0}">
                    <a href="${pageContext.request.contextPath}/notifications?action=markAllRead" class="btn btn-secondary btn-sm">
                        Mark All as Read
                    </a>
                </c:if>
            </div>
        </div>

        <div class="card">
            <div class="card-body">
                <ul class="activity-feed">
                    <c:forEach var="notif" items="${notifications}">
                        <li class="activity-item" style="padding: 12px; border-radius: var(--border-radius); ${notif.read ? 'background-color:#ffffff;' : 'background-color:#f0f9ff; border-left: 3px solid var(--primary);'}">
                            <div class="activity-avatar">
                                <c:choose>
                                    <c:when test="${notif.type == 'danger'}">
                                        <div class="avatar-initials avatar-sm" style="background:#ef4444;">!</div>
                                    </c:when>
                                    <c:when test="${notif.type == 'success'}">
                                        <div class="avatar-initials avatar-sm" style="background:#10b981;">✓</div>
                                    </c:when>
                                    <c:when test="${notif.type == 'warning'}">
                                        <div class="avatar-initials avatar-sm" style="background:#f59e0b;">⚠</div>
                                    </c:when>
                                    <c:otherwise>
                                        <div class="avatar-initials avatar-sm" style="background:#4f46e5;">i</div>
                                    </c:otherwise>
                                </c:choose>
                            </div>
                            <div class="activity-body">
                                <div style="display:flex; justify-content:space-between; align-items:center;">
                                    <h4 class="activity-title">${notif.title}</h4>
                                    <div style="display:flex; align-items:center; gap:12px;">
                                        <span class="activity-time">${notif.timeAgo}</span>
                                        <c:if test="${!notif.read}">
                                            <a href="${pageContext.request.contextPath}/notifications?action=markRead&id=${notif.id}" style="font-size:0.75rem; color:var(--primary); font-weight:600;">Mark Read</a>
                                        </c:if>
                                    </div>
                                </div>
                                <p class="activity-desc">${notif.message}</p>
                            </div>
                        </li>
                    </c:forEach>
                </ul>
                <c:if test="${empty notifications}">
                    <div style="text-align:center; padding:48px 24px; color:var(--text-muted);">
                        <div style="font-size:2rem; margin-bottom:8px;">🔔</div>
                        <p style="font-size:1.05rem; font-weight:600; margin-bottom:6px;">No notifications yet</p>
                        <p style="font-size:0.875rem;">You're all caught up! New project alerts, task assignments, and deadline warnings will appear here.</p>
                    </div>
                </c:if>
            </div>
        </div>
    </main>

<jsp:include page="common/footer.jsp" />
