package dao;

import model.Admin;
import model.ProjectManager;
import model.Role;
import model.TeamMember;
import model.User;
import java.util.*;
import java.util.stream.Collectors;

/**
 * [OOP Concept: Inheritance]
 * [OOP Concept: Polymorphism]
 * Thread-safe In-Memory implementation of UserDao extending AbstractInMemoryDao<User, Long>.
 * Stores and retrieves polymorphic User subtypes (Admin, ProjectManager, TeamMember).
 */
public class InMemoryUserDao extends AbstractInMemoryDao<User, Long> implements UserDao {

    public InMemoryUserDao() {
        seedInitialData();
    }

    private void seedInitialData() {
        // [OOP Concept: Polymorphism]
        // Storing polymorphic subclasses (ProjectManager, Admin, TeamMember) into base type User storage
        User alex = new ProjectManager(1L, "alex.morgan", "Alex Morgan", "alex.morgan@company.com", "Engineering", "AM", 120000.0, "PMP Certified");
        alex.setPassword("password123");
        save(alex);

        User sarah = new Admin(2L, "sarah.chen", "Sarah Chen", "sarah.chen@company.com", "DevOps & Cloud", "SC", "SUPER_ADMIN", true);
        sarah.setPassword("password123");
        save(sarah);

        User david = new TeamMember(3L, "david.kim", "David Kim", "david.kim@company.com", "Frontend", "DK", "React & Design Tokens", 45);
        david.setPassword("password123");
        save(david);

        User elena = new TeamMember(4L, "elena.rostova", "Elena Rostova", "elena.rostova@company.com", "Backend & Core", "ER", "Java & Distributed Systems", 55);
        elena.setPassword("password123");
        save(elena);

        User marcus = new TeamMember(5L, "marcus.vance", "Marcus Vance", "marcus.vance@company.com", "UI/UX Design", "MV", "User Experience Architecture", 35);
        marcus.setPassword("password123");
        save(marcus);

        User priya = new TeamMember(6L, "priya.sharma", "Priya Sharma", "priya.sharma@company.com", "QA & Testing", "PS", "Automated QA & Performance", 40);
        priya.setPassword("password123");
        save(priya);
    }

    @Override
    protected Long getEntityId(User entity) {
        return entity.getId();
    }

    @Override
    protected void setEntityId(User entity, Long id) {
        entity.setId(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return storage.values().stream()
                .filter(u -> username.equalsIgnoreCase(u.getUsername()))
                .findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return storage.values().stream()
                .filter(u -> email.equalsIgnoreCase(u.getEmail()))
                .findFirst();
    }

    @Override
    public List<User> findByRole(Role role) {
        if (role == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findByDepartment(String department) {
        if (department == null) return Collections.emptyList();
        return storage.values().stream()
                .filter(u -> department.equalsIgnoreCase(u.getDepartment()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<User> login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || password == null) return Optional.empty();
        String query = usernameOrEmail.trim().toLowerCase();
        return storage.values().stream()
                .filter(u -> u.isActive() &&
                        (query.equalsIgnoreCase(u.getUsername()) || query.equalsIgnoreCase(u.getEmail())) &&
                        (u.getPassword() == null || util.PasswordUtils.verifyPassword(password, u.getPassword())))
                .findFirst();
    }

    @Override
    public User register(User user, String password) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        user.setPassword(password);
        return save(user);
    }
}
