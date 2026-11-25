package com.medique.service;

import com.medique.dao.AdherenceLogDao;
import com.medique.dao.DatabaseManager;
import com.medique.dao.PrescriptionDao;
import com.medique.dao.UserDao;
import com.medique.model.AdherenceLog;
import com.medique.model.Prescription;
import com.medique.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class AdherenceAnalyticsServiceTest {
    private AdherenceLogDao logDao;
    private AdherenceAnalyticsService analyticsService;
    private int testUserId;
    private int testPrescriptionId;

    @BeforeEach
    public void setup() throws SQLException {
        String testUrl = "jdbc:sqlite:file:memdb_a_" + System.nanoTime() + "?mode=memory&cache=shared";
        DatabaseManager.setDatabaseUrlForTesting(testUrl);

        UserDao userDao = new UserDao();
        PrescriptionDao prescriptionDao = new PrescriptionDao();
        logDao = new AdherenceLogDao();
        analyticsService = new AdherenceAnalyticsService(logDao);

        User u = userDao.createUser(new User(0, "adh_user", "hash", "salt", "Adh User", "adh@test.com", "123"));
        testUserId = u.getId();

        Prescription p = prescriptionDao.createPrescription(new Prescription(0, testUserId, "Amoxicillin",
                "CAPSULE", "250 mg", "Every 8 hours", "08:00,16:00,00:00",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(5), "Dr. House", "None"));
        testPrescriptionId = p.getId();
    }

    @Test
    public void testAdherencePercentageCalculation() throws SQLException {
        LocalDateTime now = LocalDateTime.now();

        // Log 3 taken doses, 1 missed, 1 skipped -> Total 5 doses
        logDao.logDoseAction(new AdherenceLog(0, testPrescriptionId, testUserId, now.minusHours(10), now.minusHours(10), "TAKEN", ""));
        logDao.logDoseAction(new AdherenceLog(0, testPrescriptionId, testUserId, now.minusHours(8), now.minusHours(8), "TAKEN", ""));
        logDao.logDoseAction(new AdherenceLog(0, testPrescriptionId, testUserId, now.minusHours(6), now.minusHours(6), "TAKEN", ""));
        logDao.logDoseAction(new AdherenceLog(0, testPrescriptionId, testUserId, now.minusHours(4), now.minusHours(4), "MISSED", ""));
        logDao.logDoseAction(new AdherenceLog(0, testPrescriptionId, testUserId, now.minusHours(2), now.minusHours(2), "SKIPPED", ""));

        AdherenceAnalyticsService.AdherenceSummary summary = analyticsService.getTodaySummary(testUserId);

        assertEquals(5, summary.getTotalScheduled());
        assertEquals(3, summary.getTakenCount());
        assertEquals(1, summary.getMissedCount());
        assertEquals(1, summary.getSkippedCount());

        // (3 / 5) * 100 = 60.0%
        assertEquals(60.0, summary.getAdherencePercentage(), 0.01);
        assertEquals("60.0%", summary.getFormattedPercentage());
    }
}
