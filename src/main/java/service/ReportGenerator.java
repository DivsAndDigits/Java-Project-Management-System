package service;

import model.DashboardStats;
import model.ReportResult;

/**
 * [OOP Concept: Interface]
 * Contract for generating executive, financial, and sprint velocity reports.
 * Designed for polymorphic execution across different analytical reporting engines.
 */
public interface ReportGenerator {

    /**
     * Unique identifier key for this report type.
     */
    String getReportType();

    /**
     * Display title for the report.
     */
    String getReportName();

    /**
     * [OOP Concept: Polymorphism]
     * Generates analytical report results polymorphically based on current project statistics.
     *
     * @param stats aggregated domain metrics
     * @return structured report findings and KPI breakdowns
     */
    ReportResult generate(DashboardStats stats);
}
