package service;

import exception.ResourceNotFoundException;
import model.DashboardStats;
import model.ReportResult;

import java.util.ArrayList;
import java.util.List;

/**
 * [OOP Concept: Polymorphism]
 * Implementation of ReportService coordinating polymorphic report generators.
 */
public class ReportServiceImpl implements ReportService {

    private final DashboardService dashboardService;

    // [OOP Concept: Polymorphism]
    // Collection of polymorphic ReportGenerator interface instances
    private final List<ReportGenerator> reportGenerators;

    public ReportServiceImpl(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
        this.reportGenerators = new ArrayList<>();

        // Register polymorphic generators
        this.reportGenerators.add(new ExecutiveSummaryReportGenerator());
        this.reportGenerators.add(new FinancialBurnRateReportGenerator());
        this.reportGenerators.add(new SprintVelocityReportGenerator());
    }

    @Override
    public List<ReportResult> generateConsolidatedReports() {
        DashboardStats stats = dashboardService.getDashboardStatistics();
        List<ReportResult> results = new ArrayList<>();

        // [OOP Concept: Polymorphism]
        // Runtime polymorphism: Dynamic dispatch of generator.generate(stats)
        // at runtime to ExecutiveSummaryReportGenerator, FinancialBurnRateReportGenerator, etc.
        for (ReportGenerator generator : reportGenerators) {
            results.add(generator.generate(stats));
        }

        return results;
    }

    @Override
    public ReportResult generateReport(String reportType) {
        DashboardStats stats = dashboardService.getDashboardStatistics();

        for (ReportGenerator generator : reportGenerators) {
            if (generator.getReportType().equalsIgnoreCase(reportType)) {
                // [OOP Concept: Polymorphism]
                return generator.generate(stats);
            }
        }

        throw new ResourceNotFoundException("ReportGenerator", reportType);
    }
}
