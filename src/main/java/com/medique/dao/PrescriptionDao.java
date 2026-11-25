package com.medique.dao;

import com.medique.model.Prescription;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PrescriptionDao {

    public Prescription createPrescription(Prescription p) throws SQLException {
        String sql = "INSERT INTO prescriptions (user_id, medicine_name, medicine_type, dosage_amount, " +
                     "intake_instructions, dose_times, start_date, end_date, doctor_name, notes, is_active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, p.getUserId());
            pstmt.setString(2, p.getMedicineName());
            pstmt.setString(3, p.getMedicineType());
            pstmt.setString(4, p.getDosageAmount());
            pstmt.setString(5, p.getIntakeInstructions());
            pstmt.setString(6, p.getDoseTimes());
            pstmt.setString(7, p.getStartDate().toString());
            pstmt.setString(8, p.getEndDate().toString());
            pstmt.setString(9, p.getDoctorName());
            pstmt.setString(10, p.getNotes());
            pstmt.setInt(11, p.isActive() ? 1 : 0);

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    p.setId(rs.getInt(1));
                }
            }
            return p;
        }
    }

    public void updatePrescription(Prescription p) throws SQLException {
        String sql = "UPDATE prescriptions SET medicine_name = ?, medicine_type = ?, dosage_amount = ?, " +
                     "intake_instructions = ?, dose_times = ?, start_date = ?, end_date = ?, " +
                     "doctor_name = ?, notes = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP " +
                     "WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, p.getMedicineName());
            pstmt.setString(2, p.getMedicineType());
            pstmt.setString(3, p.getDosageAmount());
            pstmt.setString(4, p.getIntakeInstructions());
            pstmt.setString(5, p.getDoseTimes());
            pstmt.setString(6, p.getStartDate().toString());
            pstmt.setString(7, p.getEndDate().toString());
            pstmt.setString(8, p.getDoctorName());
            pstmt.setString(9, p.getNotes());
            pstmt.setInt(10, p.isActive() ? 1 : 0);
            pstmt.setInt(11, p.getId());
            pstmt.setInt(12, p.getUserId());

            pstmt.executeUpdate();
        }
    }

    public void deletePrescription(int id, int userId) throws SQLException {
        String sql = "DELETE FROM prescriptions WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    public Optional<Prescription> findById(int id) throws SQLException {
        String sql = "SELECT * FROM prescriptions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPrescription(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Prescription> getActivePrescriptions(int userId) throws SQLException {
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT * FROM prescriptions WHERE user_id = ? AND is_active = 1 ORDER BY medicine_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPrescription(rs));
                }
            }
        }
        return list;
    }

    public List<Prescription> getAllPrescriptions(int userId) throws SQLException {
        List<Prescription> list = new ArrayList<>();
        String sql = "SELECT * FROM prescriptions WHERE user_id = ? ORDER BY is_active DESC, medicine_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPrescription(rs));
                }
            }
        }
        return list;
    }

    private Prescription mapResultSetToPrescription(ResultSet rs) throws SQLException {
        Prescription p = new Prescription();
        p.setId(rs.getInt("id"));
        p.setUserId(rs.getInt("user_id"));
        p.setMedicineName(rs.getString("medicine_name"));
        p.setMedicineType(rs.getString("medicine_type"));
        p.setDosageAmount(rs.getString("dosage_amount"));
        p.setIntakeInstructions(rs.getString("intake_instructions"));
        p.setDoseTimes(rs.getString("dose_times"));
        p.setStartDate(LocalDate.parse(rs.getString("start_date")));
        p.setEndDate(LocalDate.parse(rs.getString("end_date")));
        p.setDoctorName(rs.getString("doctor_name"));
        p.setNotes(rs.getString("notes"));
        p.setActive(rs.getInt("is_active") == 1);
        return p;
    }
}
