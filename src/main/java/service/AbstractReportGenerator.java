package service;

import model.DashboardStats;
import model.ReportResult;
import util.DateTimeUtils;

import java.time.LocalDateTime;

/**
 * [OOP Concept: Abstraction]
 * [OOP Concept: Interface]
 * Abstract template base class providing standardized metadata and execution flow.
 * Implements the ReportGenerator interface.
 */
public abstract class AbstractReportGenerator implements ReportGenerator {

    // [OOP Concept: Encapsulation]
    private final String reportType;
    private final String reportName;
    private final String reportDescription;

    protected AbstractReportGenerator(String reportType, String reportName, String reportDescription) {
        this.reportType = reportType;
        this.reportName = reportName;
        this.reportDescription = reportDescription;
    }

    @Override
    public String getReportType() {
        return reportType;
    }

    @Override
    public String getReportName() {
        return reportName;
    }

    public String getReportDescription() {
        return reportDescription;
    }

    // =========================================================================
    // [OOP Concept: Polymorphism]
    // Template Method Pattern: Defines execution skeleton and delegates to subclass
    // =========================================================================
    @Override
    public final ReportResult generate(DashboardStats stats) {
        if (stats == null) {
            throw new IllegalArgumentException("Dashboard statistics cannot be null for report generation.");
        }

        ReportResult result = new ReportResult(getReportType(), getReportName(), getReportDescription());
        result.setGeneratedAt(DateTimeUtils.formatDateTime(LocalDateTime.now()));

        // Polymorphic invocation of subclass compilation hook
        compileMetrics(stats, result);

        return result;
    }

    // =========================================================================
    // [OOP Concept: Abstraction]
    // Subclasses must implement this abstract hook method with their unique logic
    // =========================================================================
    protected abstract void compileMetrics(DashboardStats stats, ReportResult result);
}
