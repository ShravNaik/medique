package com.medique.dao;

import com.medique.model.AdherenceLog;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AdherenceLogDao {

    public AdherenceLog logDoseAction(AdherenceLog log) throws SQLException {
        String sql = "INSERT INTO adherence_logs (prescription_id, user_id, scheduled_time, action_time, status, notes) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, log.getPrescriptionId());
            pstmt.setInt(2, log.getUserId());
            pstmt.setString(3, log.getScheduledTime().toString());
            pstmt.setString(4, log.getActionTime() != null ? log.getActionTime().toString() : null);
            pstmt.setString(5, log.getStatus());
            pstmt.setString(6, log.getNotes());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    log.setId(rs.getInt(1));
                }
            }
            return log;
        }
    }

    public List<AdherenceLog> getLogsForUser(int userId, LocalDateTime start, LocalDateTime end) throws SQLException {
        List<AdherenceLog> logs = new ArrayList<>();
        String sql = "SELECT l.*, p.medicine_name, p.dosage_amount " +
                     "FROM adherence_logs l " +
                     "JOIN prescriptions p ON l.prescription_id = p.id " +
                     "WHERE l.user_id = ? AND l.scheduled_time >= ? AND l.scheduled_time <= ? " +
                     "ORDER BY l.scheduled_time DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, start.toString());
            pstmt.setString(3, end.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AdherenceLog log = new AdherenceLog();
                    log.setId(rs.getInt("id"));
                    log.setPrescriptionId(rs.getInt("prescription_id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setScheduledTime(LocalDateTime.parse(rs.getString("scheduled_time")));
                    String actTime = rs.getString("action_time");
                    if (actTime != null && !actTime.isEmpty()) {
                        log.setActionTime(LocalDateTime.parse(actTime));
                    }
                    log.setStatus(rs.getString("status"));
                    log.setNotes(rs.getString("notes"));
                    log.setMedicineName(rs.getString("medicine_name"));
                    log.setDosageAmount(rs.getString("dosage_amount"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }

    public List<AdherenceLog> getAllLogsForUser(int userId) throws SQLException {
        List<AdherenceLog> logs = new ArrayList<>();
        String sql = "SELECT l.*, p.medicine_name, p.dosage_amount " +
                     "FROM adherence_logs l " +
                     "JOIN prescriptions p ON l.prescription_id = p.id " +
                     "WHERE l.user_id = ? " +
                     "ORDER BY l.scheduled_time DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    AdherenceLog log = new AdherenceLog();
                    log.setId(rs.getInt("id"));
                    log.setPrescriptionId(rs.getInt("prescription_id"));
                    log.setUserId(rs.getInt("user_id"));
                    log.setScheduledTime(LocalDateTime.parse(rs.getString("scheduled_time")));
                    String actTime = rs.getString("action_time");
                    if (actTime != null && !actTime.isEmpty()) {
                        log.setActionTime(LocalDateTime.parse(actTime));
                    }
                    log.setStatus(rs.getString("status"));
                    log.setNotes(rs.getString("notes"));
                    log.setMedicineName(rs.getString("medicine_name"));
                    log.setDosageAmount(rs.getString("dosage_amount"));
                    logs.add(log);
                }
            }
        }
        return logs;
    }
}
