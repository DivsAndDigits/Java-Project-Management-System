package service;

import dao.ProjectDao;
import dao.TaskDao;
import dao.UserDao;
import exception.InvalidUserException;
import exception.ResourceNotFoundException;
import exception.ValidationException;
import model.*;
import util.ValidationUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * [OOP Concept: Polymorphism]
 * Implementation of UserService managing User domain accounts and workload metrics.
 */
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final ProjectDao projectDao;
    private final TaskDao taskDao;

    public UserServiceImpl(UserDao userDao, ProjectDao projectDao, TaskDao taskDao) {
        this.userDao = userDao;
        this.projectDao = projectDao;
        this.taskDao = taskDao;
    }

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public User getUserById(Long id) {
        if (id == null) {
            // [OOP Concept: Exception Handling]
            throw new InvalidUserException("id", "User ID cannot be null.");
        }
        return userDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    public User getUserByUsername(String username) {
        if (ValidationUtils.isBlank(username)) {
            // [OOP Concept: Exception Handling]
            throw new InvalidUserException("username", "Username cannot be blank.");
        }
        return userDao.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User with username", username));
    }

    @Override
    public User createUser(User user) {
        if (user == null) {
            // [OOP Concept: Exception Handling]
            throw new InvalidUserException("User object cannot be null.");
        }
        if (ValidationUtils.isBlank(user.getUsername())) {
            throw new InvalidUserException("username", "Username is required.");
        }
        if (!ValidationUtils.isValidEmail(user.getEmail())) {
            throw new InvalidUserException("email", "A valid email format is required.");
        }
        return userDao.save(user);
    }

    @Override
    public User registerUser(User user, String password) {
        if (user == null) {
            throw new InvalidUserException("User object cannot be null.");
        }
        if (ValidationUtils.isBlank(user.getUsername())) {
            throw new InvalidUserException("username", "Username is required.");
        }
        if (!ValidationUtils.isValidLength(user.getUsername(), 3, 50)) {
            throw new InvalidUserException("username", "Username must be between 3 and 50 characters.");
        }
        if (!ValidationUtils.isValidEmail(user.getEmail())) {
            throw new InvalidUserException("email", "A valid email format is required.");
        }
        if (ValidationUtils.isBlank(password)) {
            throw new ValidationException("Password cannot be blank.");
        }
        if (!ValidationUtils.isValidLength(password, 6, 100)) {
            throw new ValidationException("Password must be at least 6 characters long.");
        }
        String hashedPassword = util.PasswordUtils.isHashed(password) ? password : util.PasswordUtils.hashPassword(password);
        return userDao.register(user, hashedPassword);
    }

    @Override
    public User loginUser(String usernameOrEmail, String password) {
        if (ValidationUtils.isBlank(usernameOrEmail)) {
            throw new ValidationException("Username or email cannot be blank.");
        }
        if (ValidationUtils.isBlank(password)) {
            throw new ValidationException("Password cannot be blank.");
        }
        User user = userDao.login(usernameOrEmail, password)
                .orElseThrow(() -> new InvalidUserException("credentials", "Invalid username/email or password."));

        // Safe in-flight password migration:
        // When a user successfully authenticates with legacy plaintext or legacy SHA-256,
        // transparently upgrade the stored hash to adaptive PBKDF2 without logging plaintext.
        if (util.PasswordUtils.needsRehash(user.getPassword())) {
            user.setPassword(util.PasswordUtils.hashPassword(password));
            userDao.update(user);
        }

        return user;
    }

    @Override
    public boolean updateUser(User user) {
        if (user == null || user.getId() == null) {
            throw new InvalidUserException("Cannot update user without a valid identifier.");
        }
        return userDao.update(user);
    }

    @Override
    public boolean deleteUser(Long id) {
        if (id == null) {
            throw new InvalidUserException("id", "User ID cannot be null.");
        }
        getUserById(id); // verify user exists first
        return userDao.deleteById(id);
    }

    @Override
    public List<User> getUsersByRole(Role role) {
        return userDao.findByRole(role);
    }

    @Override
    public List<TeamMemberSummary> getTeamMemberSummaries() {
        List<User> users = userDao.findAll();
        List<Project> projects = projectDao.findAll();
        List<Task> tasks = taskDao.findAll();

        List<TeamMemberSummary> summaries = new ArrayList<>();
        for (User u : users) {
            long activeProjectsCount = projects.stream()
                    .filter(p -> p.getStatus() == ProjectStatus.IN_PROGRESS)
                    .filter(p -> (p.getManager() != null && p.getManager().getId().equals(u.getId())) ||
                                 (p.getTeamMembers() != null && p.getTeamMembers().stream().anyMatch(m -> m.getId().equals(u.getId()))))
                    .count();

            long assignedTasksCount = tasks.stream()
                    .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(u.getId()))
                    .count();

            long completedTasksCount = tasks.stream()
                    .filter(t -> t.getAssignee() != null && t.getAssignee().getId().equals(u.getId()))
                    .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                    .count();

            // Calculate mock workload percentage
            int workload = (int) Math.min(100, Math.max(25, (assignedTasksCount * 22) + (activeProjectsCount * 15)));
            
            // [OOP Concept: Polymorphism]
            // Polymorphic method call u.getRoleTitle()
            String focus = assignedTasksCount > 0 ? "Active Sprint Delivery (" + u.getRoleTitle() + ")" : "Sprint Planning";

            summaries.add(new TeamMemberSummary(u, (int) activeProjectsCount, (int) assignedTasksCount,
                    (int) completedTasksCount, workload, focus));
        }
        return summaries;
    }

    @Override
    public User getCurrentUser() {
        // [OOP Concept: Polymorphism]
        // Returns default active ProjectManager instance
        return userDao.findById(1L).orElseGet(() ->
                new ProjectManager(1L, "alex.morgan", "Alex Morgan", "alex.morgan@company.com",
                        "Engineering", "AM", 120000.0, "PMP Certified"));
    }

    @Override
    public java.util.Map<Long, User> getUserMap() {
        // [Collections: Map]
        return userDao.findAllAsMap();
    }

    @Override
    public java.util.Set<User> getUniqueUsers() {
        // [Collections: Set]
        return new java.util.LinkedHashSet<>(userDao.findAll());
    }
}
