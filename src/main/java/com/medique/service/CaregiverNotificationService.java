package com.medique.service;

import com.medique.model.EmergencyContact;
import com.medique.model.Prescription;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class CaregiverNotificationService {

    public static class SmtpConfig {
        public String host = "smtp.gmail.com";
        public int port = 587;
        public String username = "";
        public String password = "";
        public boolean auth = true;
        public boolean startTls = true;
        public String fromEmail = "alerts@medique.local";
    }

    private SmtpConfig config;

    public CaregiverNotificationService() {
        this.config = new SmtpConfig();
    }

    public CaregiverNotificationService(SmtpConfig config) {
        this.config = config != null ? config : new SmtpConfig();
    }

    public void setConfig(SmtpConfig config) {
        this.config = config;
    }

    public SmtpConfig getConfig() {
        return config;
    }

    public CompletableFuture<Boolean> sendMissedDoseAlertAsync(
            String patientName,
            EmergencyContact caregiver,
            Prescription prescription,
            LocalDateTime scheduledTime) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendMissedDoseAlert(patientName, caregiver, prescription, scheduledTime);
            } catch (Exception e) {
                System.err.println("Caregiver alert dispatch error: " + e.getMessage());
                return false;
            }
        });
    }

    public boolean sendMissedDoseAlert(
            String patientName,
            EmergencyContact caregiver,
            Prescription prescription,
            LocalDateTime scheduledTime) {

        if (caregiver == null || caregiver.getEmail() == null || caregiver.getEmail().trim().isEmpty()) {
            System.err.println("No valid caregiver email configured for alert escalation.");
            return false;
        }

        // If credentials are not configured, log escalation locally (graceful offline / mock mode)
        if (config.username == null || config.username.trim().isEmpty()) {
            System.out.println("[ESCALATION LOG - Offline/Mock] To: " + caregiver.getEmail() +
                    " | Alert: " + patientName + " missed dose for " + prescription.getMedicineName() +
                    " (" + prescription.getDosageAmount() + ") at " + scheduledTime);
            return true;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(config.auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.startTls));
        props.put("mail.smtp.host", config.host);
        props.put("mail.smtp.port", String.valueOf(config.port));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.username, config.password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.fromEmail, "Medique Reminder Alert"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(caregiver.getEmail()));
            message.setSubject("URGENT: Medique Missed Medication Alert for " + patientName);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
            String formattedTime = scheduledTime.format(formatter);

            String emailContent = String.format(
                "Dear %s,\n\n" +
                "This is an automated safety alert from Medique.\n\n" +
                "Patient: %s\n" +
                "Medication: %s (%s)\n" +
                "Scheduled Time: %s\n" +
                "Intake Instructions: %s\n" +
                "Status: Unacknowledged for over 30 minutes.\n\n" +
                "Please check in with %s to verify medication adherence.\n\n" +
                "Best regards,\nMedique Health System",
                caregiver.getContactName() != null ? caregiver.getContactName() : "Caregiver",
                patientName,
                prescription.getMedicineName(),
                prescription.getDosageAmount(),
                formattedTime,
                prescription.getIntakeInstructions() != null ? prescription.getIntakeInstructions() : "Standard intake",
                patientName
            );

            message.setText(emailContent);
            Transport.send(message);
            System.out.println("Caregiver escalation alert successfully dispatched to " + caregiver.getEmail());
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send caregiver email: " + e.getMessage());
            return false;
        }
    }
}
