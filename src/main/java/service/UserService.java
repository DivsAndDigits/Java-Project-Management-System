package service;

import model.Role;
import model.TeamMemberSummary;
import model.User;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service contract for User profile, team capacity, and authentication.
 */
public interface UserService {

    List<User> getAllUsers();

    User getUserById(Long id);

    User getUserByUsername(String username);

    User createUser(User user);

    /**
     * Registers a new user with password.
     */
    User registerUser(User user, String password);

    /**
     * Authenticates a user by username/email and password.
     */
    User loginUser(String usernameOrEmail, String password);

    boolean updateUser(User user);

    /**
     * Deletes user by ID.
     */
    boolean deleteUser(Long id);

    List<User> getUsersByRole(Role role);

    List<TeamMemberSummary> getTeamMemberSummaries();

    User getCurrentUser();

    Map<Long, User> getUserMap();

    Set<User> getUniqueUsers();
}
