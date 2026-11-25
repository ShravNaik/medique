package com.medique.dao;

import com.medique.model.Prescription;
import com.medique.model.User;
import com.medique.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PrescriptionDaoTest {
    private PrescriptionDao prescriptionDao;
    private int testUserId;

    @BeforeEach
    public void setup() throws SQLException {
        String testUrl = "jdbc:sqlite:file:memdb_p_" + System.nanoTime() + "?mode=memory&cache=shared";
        DatabaseManager.setDatabaseUrlForTesting(testUrl);
        UserDao userDao = new UserDao();
        prescriptionDao = new PrescriptionDao();

        String salt = PasswordHasher.generateSalt();
        User u = userDao.createUser(new User(0, "presc_user", "hash", salt, "Presc User", "p@test.com", "123"));
        testUserId = u.getId();
    }

    @Test
    public void testPrescriptionCrud() throws SQLException {
        Prescription p = new Prescription(0, testUserId, "Metformin", "TABLET", "500 mg",
                "Take with meals", "08:00,20:00", LocalDate.now(), LocalDate.now().plusMonths(1),
                "Dr. Smith", "Check blood glucose weekly");

        Prescription created = prescriptionDao.createPrescription(p);
        assertTrue(created.getId() > 0);

        Optional<Prescription> fetched = prescriptionDao.findById(created.getId());
        assertTrue(fetched.isPresent());
        assertEquals("Metformin", fetched.get().getMedicineName());
        assertEquals(2, fetched.get().getDoseTimeList().size());

        // Update
        created.setDosageAmount("1000 mg");
        prescriptionDao.updatePrescription(created);

        Prescription updated = prescriptionDao.findById(created.getId()).get();
        assertEquals("1000 mg", updated.getDosageAmount());

        // List
        List<Prescription> list = prescriptionDao.getActivePrescriptions(testUserId);
        assertEquals(1, list.size());

        // Delete
        prescriptionDao.deletePrescription(created.getId(), testUserId);
        assertTrue(prescriptionDao.findById(created.getId()).isEmpty());
    }
}
