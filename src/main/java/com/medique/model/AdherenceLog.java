package com.medique.model;

import java.time.LocalDateTime;

public class AdherenceLog {
    private int id;
    private int prescriptionId;
    private int userId;
    private LocalDateTime scheduledTime;
    private LocalDateTime actionTime;
    private String status; // TAKEN, MISSED, SKIPPED, SNOOZED
    private String notes;
    private LocalDateTime createdAt;

    // Derived fields for presentation
    private String medicineName;
    private String dosageAmount;

    public AdherenceLog() {}

    public AdherenceLog(int id, int prescriptionId, int userId, LocalDateTime scheduledTime, LocalDateTime actionTime, String status, String notes) {
        this.id = id;
        this.prescriptionId = prescriptionId;
        this.userId = userId;
        this.scheduledTime = scheduledTime;
        this.actionTime = actionTime;
        this.status = status;
        this.notes = notes;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(int prescriptionId) { this.prescriptionId = prescriptionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }

    public LocalDateTime getActionTime() { return actionTime; }
    public void setActionTime(LocalDateTime actionTime) { this.actionTime = actionTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getDosageAmount() { return dosageAmount; }
    public void setDosageAmount(String dosageAmount) { this.dosageAmount = dosageAmount; }
}
