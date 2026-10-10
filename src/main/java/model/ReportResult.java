package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [OOP Concept: Encapsulation]
 * DTO holding structured results from polymorphic report generation.
 */
public class ReportResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reportType;
    private String title;
    private String description;
    private String generatedAt;
    private String healthBadge;
    private Map<String, String> summaryMetrics;
    private List<String> keyObservations;

    public ReportResult() {
        this.summaryMetrics = new LinkedHashMap<>();
        this.keyObservations = new ArrayList<>();
    }

    public ReportResult(String reportType, String title, String description) {
        this();
        this.reportType = reportType;
        this.title = title;
        this.description = description;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getHealthBadge() {
        return healthBadge;
    }

    public void setHealthBadge(String healthBadge) {
        this.healthBadge = healthBadge;
    }

    public Map<String, String> getSummaryMetrics() {
        return summaryMetrics;
    }

    public void addMetric(String key, String value) {
        this.summaryMetrics.put(key, value);
    }

    public List<String> getKeyObservations() {
        return keyObservations;
    }

    public void addObservation(String observation) {
        this.keyObservations.add(observation);
    }
}
