package service;

import model.DashboardStats;
import model.ReportResult;
import util.FormatUtils;

/**
 * [OOP Concept: Inheritance]
 * [OOP Concept: Polymorphism]
 * Concrete report generator for financial budget burn rate and resource cost allocation.
 */
public class FinancialBurnRateReportGenerator extends AbstractReportGenerator {

    public FinancialBurnRateReportGenerator() {
        super("FINANCIAL", "Financial Budget Allocation & Burn Analysis",
                "Financial tracking of authorized budgets, current burn rate, and capital expenditure forecasting.");
    }

    @Override
    protected void compileMetrics(DashboardStats stats, ReportResult result) {
        double budget = stats.getTotalBudget();
        double spent = stats.getTotalSpent();
        double remaining = Math.max(0.0, budget - spent);
        double burnPercent = budget > 0 ? (spent / budget) * 100.0 : 0.0;

        result.setHealthBadge(burnPercent > 80.0 ? "CRITICAL" : (burnPercent > 60.0 ? "WARNING" : "HEALTHY"));

        result.addMetric("Total Approved Budget", FormatUtils.formatCurrency(budget));
        result.addMetric("Total Capital Spent", FormatUtils.formatCurrency(spent));
        result.addMetric("Remaining Capital", FormatUtils.formatCurrency(remaining));
        result.addMetric("Budget Burn Rate", FormatUtils.formatPercentage(burnPercent));

        result.addObservation(String.format("Current burn is %.1f%% with %s capital reserves remaining.",
                burnPercent, FormatUtils.formatCurrency(remaining)));
        if (burnPercent > 70.0) {
            result.addObservation("Warning: High burn velocity detected. Reallocation review recommended for upcoming sprints.");
        } else {
            result.addObservation("Fiscal budget pacing is healthy within approved financial thresholds.");
        }
    }
}
