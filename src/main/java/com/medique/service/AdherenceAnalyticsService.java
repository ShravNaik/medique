package com.medique.service;

import com.medique.dao.AdherenceLogDao;
import com.medique.model.AdherenceLog;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class AdherenceAnalyticsService {
    private final AdherenceLogDao logDao;

    public AdherenceAnalyticsService() {
        this.logDao = new AdherenceLogDao();
    }

    public AdherenceAnalyticsService(AdherenceLogDao logDao) {
        this.logDao = logDao;
    }

    public static class AdherenceSummary {
        private final int totalScheduled;
        private final int takenCount;
        private final int missedCount;
        private final int skippedCount;
        private final double adherencePercentage;

        public AdherenceSummary(int totalScheduled, int takenCount, int missedCount, int skippedCount) {
            this.totalScheduled = totalScheduled;
            this.takenCount = takenCount;
            this.missedCount = missedCount;
            this.skippedCount = skippedCount;
            this.adherencePercentage = totalScheduled > 0 ? (takenCount * 100.0) / totalScheduled : 100.0;
        }

        public int getTotalScheduled() { return totalScheduled; }
        public int getTakenCount() { return takenCount; }
        public int getMissedCount() { return missedCount; }
        public int getSkippedCount() { return skippedCount; }
        public double getAdherencePercentage() { return adherencePercentage; }
        public String getFormattedPercentage() { return String.format("%.1f%%", adherencePercentage); }
    }

    public AdherenceSummary calculateSummary(int userId, LocalDate startDate, LocalDate endDate) throws SQLException {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        List<AdherenceLog> logs = logDao.getLogsForUser(userId, start, end);

        int taken = 0;
        int missed = 0;
        int skipped = 0;

        for (AdherenceLog log : logs) {
            String status = log.getStatus() != null ? log.getStatus().toUpperCase() : "";
            switch (status) {
                case "TAKEN":
                    taken++;
                    break;
                case "MISSED":
                    missed++;
                    break;
                case "SKIPPED":
                    skipped++;
                    break;
                default:
                    break;
            }
        }
        int total = taken + missed + skipped;
        return new AdherenceSummary(total, taken, missed, skipped);
    }

    public AdherenceSummary getTodaySummary(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return calculateSummary(userId, today, today);
    }

    public AdherenceSummary getLast7DaysSummary(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return calculateSummary(userId, today.minusDays(6), today);
    }

    public AdherenceSummary getLast30DaysSummary(int userId) throws SQLException {
        LocalDate today = LocalDate.now();
        return calculateSummary(userId, today.minusDays(29), today);
    }
}
