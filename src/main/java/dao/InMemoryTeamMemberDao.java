package dao;

import model.User;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Thread-safe In-Memory implementation of TeamMemberDao.
 * Preserves in-memory operation for unit tests and local development.
 */
public class InMemoryTeamMemberDao implements TeamMemberDao {

    private final UserDao userDao;
    private final ConcurrentHashMap<Long, Set<Long>> projectMembersMap = new ConcurrentHashMap<>();

    public InMemoryTeamMemberDao(UserDao userDao) {
        this.userDao = userDao;
        seedInitialData();
    }

    private void seedInitialData() {
        // Project 1 members: 3, 4, 5
        addMemberInternal(1L, 3L);
        addMemberInternal(1L, 4L);
        addMemberInternal(1L, 5L);

        // Project 2 members: 5, 3, 6
        addMemberInternal(2L, 5L);
        addMemberInternal(2L, 3L);
        addMemberInternal(2L, 6L);

        // Project 3 members: 4, 6
        addMemberInternal(3L, 4L);
        addMemberInternal(3L, 6L);

        // Project 4 members: 3, 4
        addMemberInternal(4L, 3L);
        addMemberInternal(4L, 4L);

        // Project 5 members: 5
        addMemberInternal(5L, 5L);
    }

    private boolean addMemberInternal(Long projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        Set<Long> members = projectMembersMap.computeIfAbsent(projectId, k -> new CopyOnWriteArraySet<>());
        return members.add(userId);
    }

    @Override
    public boolean addTeamMember(Long projectId, Long userId) {
        return addMemberInternal(projectId, userId);
    }

    @Override
    public boolean removeTeamMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        Set<Long> members = projectMembersMap.get(projectId);
        return members != null && members.remove(userId);
    }

    @Override
    public Set<User> findProjectTeam(Long projectId) {
        if (projectId == null || userDao == null) return Collections.emptySet();
        Set<Long> userIds = projectMembersMap.get(projectId);
        if (userIds == null || userIds.isEmpty()) return Collections.emptySet();

        return userIds.stream()
                .map(id -> userDao.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isMember(Long projectId, Long userId) {
        if (projectId == null || userId == null) return false;
        Set<Long> members = projectMembersMap.get(projectId);
        return members != null && members.contains(userId);
    }

    @Override
    public int countByProjectId(Long projectId) {
        if (projectId == null) return 0;
        Set<Long> members = projectMembersMap.get(projectId);
        return members != null ? members.size() : 0;
    }

    @Override
    public void removeAllForProject(Long projectId) {
        if (projectId != null) {
            projectMembersMap.remove(projectId);
        }
    }
}
