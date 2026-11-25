package com.medique.service;

import com.medique.dao.DatabaseManager;
import com.medique.dao.PrescriptionDao;
import com.medique.dao.UserDao;
import com.medique.model.Prescription;
import com.medique.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class BackendSyncServiceTest {
    private BackendSyncService syncService;
    private int testUserId;

    @BeforeEach
    public void setup() throws SQLException {
        String testUrl = "jdbc:sqlite:file:memdb_sync_" + System.nanoTime() + "?mode=memory&cache=shared";
        DatabaseManager.setDatabaseUrlForTesting(testUrl);

        UserDao userDao = new UserDao();
        PrescriptionDao prescriptionDao = new PrescriptionDao();
        syncService = new BackendSyncService();

        User u = userDao.createUser(new User(0, "sync_user", "hash", "salt", "Sync User", "sync@test.com", "555-1234"));
        testUserId = u.getId();

        prescriptionDao.createPrescription(new Prescription(0, testUserId, "Atorvastatin", "TABLET", "20 mg",
                "Bedtime", "21:00", LocalDate.now(), LocalDate.now().plusMonths(6), "Dr. Who", "Cholesterol"));
    }

    @Test
    public void testJsonPayloadExport() throws Exception {
        String json = syncService.exportSyncPayloadJson(testUserId);
        assertNotNull(json);
        assertTrue(json.contains("Atorvastatin"), "Payload must contain prescription medication name");
        assertTrue(json.contains("20 mg"), "Payload must contain dosage amount");
        assertTrue(json.contains("prescriptions"), "Payload must contain prescriptions array");
    }

    @Test
    public void testOfflineQueueing() {
        assertEquals(0, syncService.getPendingQueueCount());
        syncService.queueSyncLocally("PRESCRIPTION", 101, "{\"action\":\"sync_test\"}");
        assertEquals(1, syncService.getPendingQueueCount(), "Pending queue count should increment after local queueing");
    }
}
