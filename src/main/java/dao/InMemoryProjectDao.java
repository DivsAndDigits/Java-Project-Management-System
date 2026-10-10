package dao;

import model.Priority;
import model.Project;
import model.ProjectStatus;
import model.User;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * [OOP Concept: Inheritance]
 * Thread-safe In-Memory implementation of ProjectDao extending AbstractInMemoryDao<Project, Long>.
 */
public class InMemoryProjectDao extends AbstractInMemoryDao<Project, Long> implements ProjectDao {

    private final UserDao userDao;

    public InMemoryProjectDao(UserDao userDao) {
        this.userDao = userDao;
        seedInitialData(userDao);
    }

    private void seedInitialData(UserDao userDao) {
        User alex = userDao.findById(1L).orElse(null);
        User david = userDao.findById(3L).orElse(null);
        User elena = userDao.findById(4L).orElse(null);
        User marcus = userDao.findById(5L).orElse(null);
        User priya = userDao.findById(6L).orElse(null);

        // Project 1: Cloud Migration Platform
        Project p1 = new Project(1L, "PRJ-101", "Cloud Infrastructure Modernization",
                "Migrate legacy on-prem services to microservices on AWS with Kubernetes orchestration.",
                ProjectStatus.IN_PROGRESS, Priority.URGENT, alex,
                LocalDate.now().minusDays(30), LocalDate.now().plusDays(45),
                85000.0, 52000.0, 68, 24, 16);
        if (david != null) p1.addTeamMember(david);
        if (elena != null) p1.addTeamMember(elena);
        if (marcus != null) p1.addTeamMember(marcus);
        storage.put(p1.getId(), p1);

        // Project 2: NextGen Mobile Application
        Project p2 = new Project(2L, "PRJ-102", "NextGen Mobile Banking App",
                "Redesign and launch cross-platform mobile client with biometric security and real-time alerts.",
                ProjectStatus.IN_PROGRESS, Priority.HIGH, alex,
                LocalDate.now().minusDays(15), LocalDate.now().plusDays(60),
                120000.0, 48000.0, 42, 38, 16);
        if (marcus != null) p2.addTeamMember(marcus);
        if (david != null) p2.addTeamMember(david);
        if (priya != null) p2.addTeamMember(priya);
        storage.put(p2.getId(), p2);

        // Project 3: Enterprise Analytics Dashboard
        Project p3 = new Project(3L, "PRJ-103", "Enterprise Business Analytics",
                "Develop high-performance reporting dashboards with real-time streaming data ingestion.",
                ProjectStatus.PLANNING, Priority.MEDIUM, alex,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(90),
                65000.0, 5000.0, 15, 18, 2);
        if (elena != null) p3.addTeamMember(elena);
        if (priya != null) p3.addTeamMember(priya);
        storage.put(p3.getId(), p3);

        // Project 4: Security Compliance Audit
        Project p4 = new Project(4L, "PRJ-104", "ISO-27001 Security Audit & Remediation",
                "Comprehensive audit across all production APIs, penetration testing, and compliance documentation.",
                ProjectStatus.COMPLETED, Priority.HIGH, alex,
                LocalDate.now().minusDays(75), LocalDate.now().minusDays(5),
                45000.0, 43500.0, 100, 14, 14);
        if (david != null) p4.addTeamMember(david);
        if (elena != null) p4.addTeamMember(elena);
        storage.put(p4.getId(), p4);

        // Project 5: Customer Portal Redesign
        Project p5 = new Project(5L, "PRJ-105", "Customer Support Portal 2.0",
                "Replatform the self-service ticket resolution portal with automated AI response workflows.",
                ProjectStatus.ON_HOLD, Priority.LOW, alex,
                LocalDate.now().minusDays(40), LocalDate.now().plusDays(20),
                30000.0, 18000.0, 55, 12, 6);
        if (marcus != null) p5.addTeamMember(marcus);
        storage.put(p5.getId(), p5);
    }

    @Override
    protected Long getEntityId(Project entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(Project entity, Long id) {
        entity.setId(id);
    }

    @Override
    public Project save(Project entity) {
        if (entity.getId() == null) {
            super.save(entity);
            if (entity.getCode() == null || entity.getCode().isBlank()) {
                entity.setCode("PRJ-" + entity.getId());
            }
        } else {
            storage.put(entity.getId(), entity);
        }
        return entity;
    }

    @Override
    public List<Project> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(Project::getId))
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> findByStatus(ProjectStatus status) {
        if (status == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(p -> p.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> findByPriority(Priority priority) {
        if (priority == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(p -> p.getPriority() == priority)
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> findRecent(int limit) {
        return storage.values().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> findByManagerId(Long managerId) {
        if (managerId == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(p -> p.getManager() != null && managerId.equals(p.getManager().getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> searchByName(String keyword) {
        if (keyword == null || keyword.isBlank()) return findAll();
        String lower = keyword.toLowerCase();
        return storage.values().stream()
                .filter(p -> p.getName().toLowerCase().contains(lower) ||
                             p.getCode().toLowerCase().contains(lower) ||
                             (p.getDescription() != null && p.getDescription().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    @Override
    public int countByStatus(ProjectStatus status) {
        if (status == null) return 0;
        return (int) storage.values().stream()
                .filter(p -> p.getStatus() == status)
                .count();
    }

    @Override
    public Project createProjectWithTeam(Project project, Set<Long> memberUserIds) {
        Project saved = save(project);
        if (memberUserIds != null && this.userDao != null) {
            for (Long uid : memberUserIds) {
                userDao.findById(uid).ifPresent(saved::addTeamMember);
            }
        }
        return saved;
    }
}
