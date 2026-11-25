package com.medique.service;

import com.medique.model.EmergencyContact;
import com.medique.model.Prescription;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CaregiverNotificationServiceTest {

    @Test
    public void testOfflineEscalationGracefulHandling() {
        CaregiverNotificationService service = new CaregiverNotificationService();
        // Default empty credentials triggers offline mock/log mode
        EmergencyContact contact = new EmergencyContact(1, 10, "Sarah Caregiver", "Spouse", "sarah@example.com", "555-9999", true);
        Prescription p = new Prescription(1, 10, "Lisinopril", "TABLET", "10 mg", "Morning", "08:00", LocalDate.now(), LocalDate.now().plusMonths(3), "Dr. Adams", "");

        boolean result = service.sendMissedDoseAlert("Test Patient", contact, p, LocalDateTime.now());
        assertTrue(result, "Offline escalation should succeed gracefully by logging without crashing");
    }

    @Test
    public void testMissingCaregiverEmailFailsGracefully() {
        CaregiverNotificationService service = new CaregiverNotificationService();
        EmergencyContact contact = new EmergencyContact(1, 10, "No Email Contact", "Friend", "", "555-0000", true);
        Prescription p = new Prescription(1, 10, "Aspirin", "TABLET", "81 mg", "", "09:00", LocalDate.now(), LocalDate.now().plusDays(10), "", "");

        boolean result = service.sendMissedDoseAlert("Test Patient", contact, p, LocalDateTime.now());
        assertFalse(result, "Escalation without email should return false gracefully");
    }
}
