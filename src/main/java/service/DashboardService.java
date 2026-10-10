package service;

import model.DashboardStats;

/**
 * Service contract for computing consolidated metrics and data for the executive dashboard.
 */
public interface DashboardService {

    DashboardStats getDashboardStatistics();
}
