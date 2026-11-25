package com.medique.dao;

import com.medique.model.EmergencyContact;
import com.medique.model.User;
import com.medique.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerAndUserDaoTest {
    private UserDao userDao;

    @BeforeEach
    public void setup() {
        // Use unique in-memory database for isolation
        String testUrl = "jdbc:sqlite:file:memdb_" + System.nanoTime() + "?mode=memory&cache=shared";
        DatabaseManager.setDatabaseUrlForTesting(testUrl);
        userDao = new UserDao();
    }

    @Test
    public void testSchemaInitialization() throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")) {
            assertTrue(rs.next(), "Table 'users' must exist in initialized database");
        }
    }

    @Test
    public void testUserCreationAndLookup() throws SQLException {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword("testPass123", salt);

        User user = new User(0, "john_doe", hash, salt, "John Doe", "john@example.com", "123-4567");
        User created = userDao.createUser(user);

        assertTrue(created.getId() > 0, "User ID should be generated");

        Optional<User> found = userDao.findByUsername("john_doe");
        assertTrue(found.isPresent(), "User should be found by username");
        assertEquals("John Doe", found.get().getFullName());
    }

    @Test
    public void testEmergencyContactPersistence() throws SQLException {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPassword("pass", salt);
        User user = userDao.createUser(new User(0, "patient_care", hash, salt, "Patient With Caregiver", "care@example.com", "555-0100"));

        EmergencyContact contact = new EmergencyContact(0, user.getId(), "Jane Caregiver", "Daughter", "jane@example.com", "555-0200", true);
        EmergencyContact saved = userDao.saveEmergencyContact(contact);

        assertTrue(saved.getId() > 0);

        List<EmergencyContact> contacts = userDao.getEmergencyContacts(user.getId());
        assertEquals(1, contacts.size());
        assertEquals("Jane Caregiver", contacts.get(0).getContactName());
        assertEquals("jane@example.com", contacts.get(0).getEmail());
    }
}
