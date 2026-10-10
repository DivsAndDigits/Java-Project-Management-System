package service;

import dao.*;
import util.DatabaseConnectionManager;

/**
 * ==============================================================================
 * Thread-safe Service Locator & DAO Factory Pattern (7 marks foundation)
 * ==============================================================================
 * 
 * Provides centralized access to application service singletons across Servlets.
 * Enables clean switching between In-Memory DAOs (for unit tests / development)
 * and concrete JDBC DAOs (for production MySQL 8.x persistence).
 * 
 * Switch mechanism:
 * 1. Configuration in src/main/resources/db.properties (dao.mode=memory or dao.mode=jdbc).
 * 2. JVM System Property override: -Ddao.mode=jdbc.
 * 3. Programmatic configuration via ServiceFactory.setDaoMode(DaoMode.JDBC).
 */
public final class ServiceFactory {

    public enum DaoMode {
        MEMORY,
        JDBC
    }

    private static volatile ServiceFactory instance;
    private static volatile DaoMode forcedMode = null;

    private final DaoMode daoMode;
    private final UserDao userDao;
    private final ProjectDao projectDao;
    private final TaskDao taskDao;
    private final NotificationDao notificationDao;
    private final TeamMemberDao teamMemberDao;

    private final UserService userService;
    private final ProjectService projectService;
    private final TaskService taskService;
    private final DashboardService dashboardService;
    private final NotificationService notificationService;
    private final ReportService reportService;

    private ServiceFactory() {
        this(resolveDefaultDaoMode());
    }

    private ServiceFactory(DaoMode mode) {
        this.daoMode = mode != null ? mode : DaoMode.MEMORY;

        if (this.daoMode == DaoMode.JDBC) {
            DatabaseConnectionManager connMgr = DatabaseConnectionManager.getInstance();
            this.userDao = new JdbcUserDao(connMgr);
            this.teamMemberDao = new JdbcTeamMemberDao(connMgr);
            this.projectDao = new JdbcProjectDao(connMgr, this.teamMemberDao);
            this.taskDao = new JdbcTaskDao(connMgr);
            this.notificationDao = new JdbcNotificationDao(connMgr);
        } else {
            this.userDao = new InMemoryUserDao();
            this.teamMemberDao = new InMemoryTeamMemberDao(this.userDao);
            this.projectDao = new InMemoryProjectDao(this.userDao);
            this.taskDao = new InMemoryTaskDao(this.userDao);
            this.notificationDao = new InMemoryNotificationDao();
        }

        // Initialize Services
        this.notificationService = new NotificationServiceImpl(notificationDao);
        this.userService = new UserServiceImpl(userDao, projectDao, taskDao);
        this.projectService = new ProjectServiceImpl(projectDao, teamMemberDao, taskDao, notificationService);
        this.taskService = new TaskServiceImpl(taskDao, projectDao, notificationService);
        this.dashboardService = new DashboardServiceImpl(projectDao, taskDao, userDao, userService);
        this.reportService = new ReportServiceImpl(dashboardService);
    }

    private static DaoMode resolveDefaultDaoMode() {
        if (forcedMode != null) {
            return forcedMode;
        }

        String sysProp = System.getProperty("dao.mode");
        if (sysProp != null && !sysProp.isBlank()) {
            try {
                return DaoMode.valueOf(sysProp.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        String cfgMode = DatabaseConnectionManager.getInstance().getDaoMode();
        if ("jdbc".equalsIgnoreCase(cfgMode)) {
            return DaoMode.JDBC;
        }
        return DaoMode.MEMORY;
    }

    public static ServiceFactory getInstance() {
        if (instance == null) {
            synchronized (ServiceFactory.class) {
                if (instance == null) {
                    instance = new ServiceFactory();
                }
            }
        }
        return instance;
    }

    /**
     * Programmatically switches the DAO mode (e.g. for testing between In-Memory and JDBC).
     *
     * @param mode target DaoMode (MEMORY or JDBC)
     */
    public static synchronized void setDaoMode(DaoMode mode) {
        forcedMode = mode;
        instance = new ServiceFactory(mode);
    }

    /**
     * Resets the factory singleton instance.
     */
    public static synchronized void reset() {
        forcedMode = null;
        instance = null;
    }

    public DaoMode getDaoMode() {
        return daoMode;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public ProjectDao getProjectDao() {
        return projectDao;
    }

    public TaskDao getTaskDao() {
        return taskDao;
    }

    public NotificationDao getNotificationDao() {
        return notificationDao;
    }

    public TeamMemberDao getTeamMemberDao() {
        return teamMemberDao;
    }

    public UserService getUserService() {
        return userService;
    }

    public ProjectService getProjectService() {
        return projectService;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }

    public ReportService getReportService() {
        return reportService;
    }

    public service.concurrent.NotificationCoordinator getNotificationCoordinator() {
        return service.concurrent.AppSchedulerManager.getInstance().getNotificationCoordinator();
    }

    public service.concurrent.NotificationBatchProcessor getNotificationBatchProcessor() {
        return service.concurrent.AppSchedulerManager.getInstance().getBatchProcessor();
    }

    public service.concurrent.AppSchedulerManager getAppSchedulerManager() {
        return service.concurrent.AppSchedulerManager.getInstance();
    }
}
