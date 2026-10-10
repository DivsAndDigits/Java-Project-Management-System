package dao;

import model.Role;
import model.User;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object contract for User and Team Member entities.
 */
public interface UserDao extends BaseDao<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByDepartment(String department);
    Optional<User> login(String usernameOrEmail, String password);

    User register(User user, String password);
}
