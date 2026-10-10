package service;

import model.ReportResult;
import java.util.List;

/**
 * [OOP Concept: Interface]
 * Service contract for coordinating analytical report generation across the application.
 */
public interface ReportService {

    /**
     * [OOP Concept: Polymorphism]
     * Generates all consolidated analytical reports by dispatching polymorphically
     * to all registered ReportGenerator implementations.
     *
     * @return list of generated report results
     */
    List<ReportResult> generateConsolidatedReports();

    /**
     * Generates a specific report by its unique type key.
     *
     * @param reportType e.g., "EXECUTIVE", "FINANCIAL", "VELOCITY"
     * @return generated report result
     */
    ReportResult generateReport(String reportType);
}
