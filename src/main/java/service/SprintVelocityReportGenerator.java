package service;

import model.DashboardStats;
import model.ReportResult;
import util.FormatUtils;

/**
 * [OOP Concept: Inheritance]
 * [OOP Concept: Polymorphism]
 * Concrete report generator for sprint velocity, task throughput, and blocker diagnostics.
 */
public class SprintVelocityReportGenerator extends AbstractReportGenerator {

    public SprintVelocityReportGenerator() {
        super("VELOCITY", "Sprint Velocity & Work Item Throughput",
                "Analysis of developer task throughput, cycle times, in-flight work items, and blockers.");
    }

    @Override
    protected void compileMetrics(DashboardStats stats, ReportResult result) {
        int total = stats.getTotalTasks();
        int completed = stats.getCompletedTasks();
        int inProgress = stats.getInProgressTasks();
        int overdue = stats.getOverdueTasks();

        double completionRate = total > 0 ? (completed * 100.0 / total) : 0.0;
        result.setHealthBadge(overdue > 2 ? "WARNING" : "HEALTHY");

        result.addMetric("Total Work Items", String.valueOf(total));
        result.addMetric("Active Tasks In-Flight", String.valueOf(inProgress));
        result.addMetric("Completed Milestones", String.valueOf(completed));
        result.addMetric("Completion Ratio", FormatUtils.formatPercentage(completionRate));

        result.addObservation(String.format("Sprint completion ratio is currently %s.",
                FormatUtils.formatPercentage(completionRate)));
        result.addObservation(String.format("%d tasks are currently in active execution across cross-functional engineering pods.",
                inProgress));
        if (overdue > 0) {
            result.addObservation(String.format("Alert: %d items are exceeding expected sprint timeline targets.", overdue));
        } else {
            result.addObservation("Zero sprint blockers detected; sprint velocity is optimal.");
        }
    }
}
