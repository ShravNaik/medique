package com.medique.dao;

import com.medique.model.EmergencyContact;
import com.medique.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public User createUser(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, salt, full_name, email, phone) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, user.getUsername().trim().toLowerCase());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getSalt());
            pstmt.setString(4, user.getFullName());
            pstmt.setString(5, user.getEmail());
            pstmt.setString(6, user.getPhone());

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    user.setId(rs.getInt(1));
                }
            }
            return user;
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(username) = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username.trim().toLowerCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void updateUserPreferences(int userId, String themePreference, String fontScale) throws SQLException {
        String sql = "UPDATE users SET theme_preference = ?, font_scale = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, themePreference);
            pstmt.setString(2, fontScale);
            pstmt.setInt(3, userId);
            pstmt.executeUpdate();
        }
    }

    // Emergency Contacts Management
    public EmergencyContact saveEmergencyContact(EmergencyContact contact) throws SQLException {
        String sql = "INSERT INTO emergency_contacts (user_id, contact_name, relationship, email, phone, is_primary) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, contact.getUserId());
            pstmt.setString(2, contact.getContactName());
            pstmt.setString(3, contact.getRelationship());
            pstmt.setString(4, contact.getEmail());
            pstmt.setString(5, contact.getPhone());
            pstmt.setInt(6, contact.isPrimary() ? 1 : 0);

            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    contact.setId(rs.getInt(1));
                }
            }
            return contact;
        }
    }

    public List<EmergencyContact> getEmergencyContacts(int userId) throws SQLException {
        List<EmergencyContact> contacts = new ArrayList<>();
        String sql = "SELECT * FROM emergency_contacts WHERE user_id = ? ORDER BY is_primary DESC, id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    EmergencyContact c = new EmergencyContact(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("contact_name"),
                        rs.getString("relationship"),
                        rs.getString("email"),
                        rs.getString("phone"),
                        rs.getInt("is_primary") == 1
                    );
                    contacts.add(c);
                }
            }
        }
        return contacts;
    }

    public void deleteEmergencyContact(int contactId, int userId) throws SQLException {
        String sql = "DELETE FROM emergency_contacts WHERE id = ? AND user_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, contactId);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setSalt(rs.getString("salt"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPhone(rs.getString("phone"));
        user.setThemePreference(rs.getString("theme_preference"));
        user.setFontScale(rs.getString("font_scale"));
        return user;
    }
}
