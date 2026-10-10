package service;

import dao.InMemoryProjectDao;
import dao.InMemoryUserDao;
import dao.ProjectDao;
import dao.UserDao;
import exception.ProjectNotFoundException;
import exception.UnauthorizedAccessException;
import exception.ValidationException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ProjectService Unit Tests")
public class ProjectServiceTest {

    private ProjectService projectService;
    private ProjectDao projectDao;
    private UserDao userDao;

    @BeforeEach
    void setUp() {
        userDao = new InMemoryUserDao();
        projectDao = new InMemoryProjectDao(userDao);
        projectService = new ProjectServiceImpl(projectDao);
    }

    @Test
    @DisplayName("Should retrieve all initial seed projects")
    void testGetAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        assertNotNull(projects, "Project list should not be null");
        assertFalse(projects.isEmpty(), "Should contain seed projects");
        assertTrue(projects.size() >= 5, "Should have at least 5 initial projects");
    }

    @Test
    @DisplayName("Should find project by existing ID")
    void testGetProjectById() {
        Project project = projectService.getProjectById(1L);
        assertNotNull(project);
        assertEquals(1L, project.getId());
        assertEquals("Cloud Infrastructure Modernization", project.getName());
    }

    @Test
    @DisplayName("Should throw ProjectNotFoundException for non-existent project ID")
    void testGetProjectByIdNotFound() {
        // [OOP Concept: Exception Handling]
        assertThrows(ProjectNotFoundException.class, () -> projectService.getProjectById(9999L));
    }

    @Test
    @DisplayName("Should create new project successfully with authorized ProjectManager")
    void testCreateProjectWithAuthorizedManager() {
        User pm = userDao.findById(1L).orElseThrow();

        Project newProject = new Project();
        newProject.setName("AI Workflow Engine");
        newProject.setCode("PRJ-888");
        newProject.setStatus(ProjectStatus.PLANNING);
        newProject.setPriority(Priority.HIGH);
        newProject.setBudget(75000.0);
        newProject.setManager(pm);
        newProject.setStartDate(LocalDate.now());
        newProject.setEndDate(LocalDate.now().plusMonths(3));

        Project created = projectService.createProject(newProject);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("AI Workflow Engine", created.getName());

        Project retrieved = projectService.getProjectById(created.getId());
        assertEquals("PRJ-888", retrieved.getCode());
    }

    @Test
    @DisplayName("Should throw UnauthorizedAccessException when TeamMember tries to manage a project")
    void testCreateProjectUnauthorizedTeamMember() {
        // [OOP Concept: Polymorphism] - TeamMember has canManageProjects() == false
        User teamMember = userDao.findById(3L).orElseThrow();
        assertTrue(teamMember instanceof TeamMember, "User should be an instance of TeamMember");
        assertFalse(teamMember.canManageProjects(), "TeamMember should not be permitted to manage projects");

        Project unauthorizedProject = new Project();
        unauthorizedProject.setName("Unauthorized Initiative");
        unauthorizedProject.setManager(teamMember);

        // [OOP Concept: Exception Handling]
        assertThrows(UnauthorizedAccessException.class, () -> projectService.createProject(unauthorizedProject));
    }

    @Test
    @DisplayName("Should throw ValidationException when creating null project or empty name")
    void testCreateProjectValidation() {
        assertThrows(ValidationException.class, () -> projectService.createProject(null));

        Project invalidProject = new Project();
        invalidProject.setName("   ");
        assertThrows(ValidationException.class, () -> projectService.createProject(invalidProject));
    }

    @Test
    @DisplayName("Should filter projects by status")
    void testFilterByStatus() {
        List<Project> inProgress = projectService.getProjectsByStatus(ProjectStatus.IN_PROGRESS);
        assertNotNull(inProgress);
        assertTrue(inProgress.stream().allMatch(p -> p.getStatus() == ProjectStatus.IN_PROGRESS));
    }
}
