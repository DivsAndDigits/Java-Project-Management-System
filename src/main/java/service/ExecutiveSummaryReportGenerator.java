package service;

import model.DashboardStats;
import model.ReportResult;

/**
 * [OOP Concept: Inheritance]
 * [OOP Concept: Polymorphism]
 * Concrete report generator for Executive Milestone & Project Delivery summaries.
 */
public class ExecutiveSummaryReportGenerator extends AbstractReportGenerator {

    public ExecutiveSummaryReportGenerator() {
        super("EXECUTIVE", "Executive Milestone & Portfolio Summary",
                "Consolidated overview of active initiatives, strategic roadmap progress, and portfolio health.");
    }

    @Override
    protected void compileMetrics(DashboardStats stats, ReportResult result) {
        result.setHealthBadge(stats.getOverdueTasks() > 3 ? "WARNING" : "HEALTHY");

        result.addMetric("Active Initiatives", String.valueOf(stats.getActiveProjects()));
        result.addMetric("Total Projects Managed", String.valueOf(stats.getTotalProjects()));
        result.addMetric("Cumulative Milestone Progress", stats.getOverallProgressPercentage() + "%");
        result.addMetric("Engineering Resource Pool", stats.getTeamMembersCount() + " Members");

        result.addObservation(String.format("Portfolio progress is currently at %d%% across %d active projects.",
                stats.getOverallProgressPercentage(), stats.getActiveProjects()));
        result.addObservation(String.format("A total of %d tasks are completed out of %d planned work items.",
                stats.getCompletedTasks(), stats.getTotalTasks()));
        if (stats.getOverdueTasks() > 0) {
            result.addObservation(String.format("Attention: %d work items are flagged as overdue and require sprint intervention.",
                    stats.getOverdueTasks()));
        }
    }
}
