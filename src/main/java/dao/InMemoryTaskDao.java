package dao;

import model.Priority;
import model.Task;
import model.TaskStatus;
import model.User;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * [OOP Concept: Inheritance]
 * Thread-safe In-Memory implementation of TaskDao extending AbstractInMemoryDao<Task, Long>.
 */
public class InMemoryTaskDao extends AbstractInMemoryDao<Task, Long> implements TaskDao {

    private final UserDao userDao;

    public InMemoryTaskDao(UserDao userDao) {
        this.userDao = userDao;
        seedInitialData(userDao);
    }

    private void seedInitialData(UserDao userDao) {
        User alex = userDao.findById(1L).orElse(null);
        User david = userDao.findById(3L).orElse(null);
        User elena = userDao.findById(4L).orElse(null);
        User marcus = userDao.findById(5L).orElse(null);
        User priya = userDao.findById(6L).orElse(null);

        // Tasks for Cloud Infrastructure Modernization (ProjectId: 1)
        storage.put(1L, new Task(1L, 1L, "Cloud Infrastructure Modernization",
                "Configure Kubernetes EKS Cluster & Ingress Controllers",
                "Setup production and staging cluster manifests with Helm charts and TLS cert-manager.",
                TaskStatus.IN_PROGRESS, Priority.URGENT, elena, alex,
                LocalDate.now().plusDays(2), 32, 20));

        storage.put(2L, new Task(2L, 1L, "Cloud Infrastructure Modernization",
                "Implement IAM Least-Privilege Policies",
                "Audit cross-account IAM role assumptions and establish boundary permissions.",
                TaskStatus.TODO, Priority.HIGH, david, alex,
                LocalDate.now().plusDays(5), 16, 0));

        // Tasks for NextGen Mobile Banking App (ProjectId: 2)
        storage.put(3L, new Task(3L, 2L, "NextGen Mobile Banking App",
                "Refactor Biometric Authentication Flow (FaceID / Fingerprint)",
                "Support Android Keystore and iOS Secure Enclave fallbacks with encrypted session tokens.",
                TaskStatus.IN_PROGRESS, Priority.HIGH, david, alex,
                LocalDate.now().plusDays(3), 24, 14));

        storage.put(4L, new Task(4L, 2L, "NextGen Mobile Banking App",
                "Design Dark-Mode System UI Tokens & Components",
                "Create reusable Figma token mappings and sync with Flutter design system stylesheets.",
                TaskStatus.COMPLETED, Priority.MEDIUM, marcus, alex,
                LocalDate.now().minusDays(2), 20, 20));

        storage.put(5L, new Task(5L, 2L, "NextGen Mobile Banking App",
                "Automated End-to-End Regression Test Suite",
                "Write Appium test scripts for core transaction flows and wire transfer confirmations.",
                TaskStatus.IN_REVIEW, Priority.HIGH, priya, alex,
                LocalDate.now().plusDays(4), 28, 22));

        // Tasks for Enterprise Business Analytics (ProjectId: 3)
        storage.put(6L, new Task(6L, 3L, "Enterprise Business Analytics",
                "Design Event-Driven WebSocket Ingestion Pipeline",
                "Benchmark Apache Kafka vs AWS Kinesis throughput for real-time charting feeds.",
                TaskStatus.TODO, Priority.MEDIUM, elena, alex,
                LocalDate.now().plusDays(10), 40, 4));

        // Overdue task sample
        storage.put(7L, new Task(7L, 1L, "Cloud Infrastructure Modernization",
                "Database Disaster Recovery & Cross-Region Backup Drill",
                "Simulate failover to secondary AWS region and document recovery time objective (RTO).",
                TaskStatus.IN_PROGRESS, Priority.URGENT, elena, alex,
                LocalDate.now().minusDays(1), 16, 12));

        storage.put(8L, new Task(8L, 2L, "NextGen Mobile Banking App",
                "Push Notification Service Integration",
                "Integrate Firebase Cloud Messaging and Apple APNs with retry queue backoff.",
                TaskStatus.TODO, Priority.MEDIUM, david, alex,
                LocalDate.now().plusDays(7), 18, 0));
    }

    @Override
    protected Long getEntityId(Task entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(Task entity, Long id) {
        entity.setId(id);
    }

    @Override
    public List<Task> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(Task::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByProjectId(Long projectId) {
        if (projectId == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(t -> projectId.equals(t.getProjectId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByAssigneeId(Long assigneeId) {
        if (assigneeId == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(t -> t.getAssignee() != null && assigneeId.equals(t.getAssignee().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByStatus(TaskStatus status) {
        if (status == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(t -> t.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findByPriority(Priority priority) {
        if (priority == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(t -> t.getPriority() == priority)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findPriorityTasks(int limit) {
        return storage.values().stream()
                .filter(t -> t.getStatus() != TaskStatus.COMPLETED)
                .sorted((a, b) -> {
                    int pComp = Integer.compare(b.getPriority().ordinal(), a.getPriority().ordinal());
                    if (pComp != 0) return pComp;
                    return a.getDueDate().compareTo(b.getDueDate());
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> findOverdueTasks() {
        LocalDate today = LocalDate.now();
        return storage.values().stream()
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(today) && t.getStatus() != TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    @Override
    public List<Task> searchByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        String lower = keyword.toLowerCase();
        return storage.values().stream()
                .filter(t -> (t.getTitle() != null && t.getTitle().toLowerCase().contains(lower)) ||
                             (t.getDescription() != null && t.getDescription().toLowerCase().contains(lower)) ||
                             (t.getProjectName() != null && t.getProjectName().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    @Override
    public int countByStatus(TaskStatus status) {
        if (status == null) return 0;
        return (int) storage.values().stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    @Override
    public boolean assignTask(Long taskId, Long assigneeId) {
        if (taskId == null) return false;
        Task task = findById(taskId).orElse(null);
        if (task == null) return false;
        if (assigneeId != null && userDao != null) {
            userDao.findById(assigneeId).ifPresent(task::setAssignee);
        } else {
            task.setAssignee(null);
        }
        return update(task);
    }
}
