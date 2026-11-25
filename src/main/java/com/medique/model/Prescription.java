package com.medique.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Prescription {
    private int id;
    private int userId;
    private String medicineName;
    private String medicineType; // TABLET, SYRUP, INJECTION, CAPSULE, DROPS, OTHER
    private String dosageAmount; // e.g. "1 tablet", "5 ml"
    private String intakeInstructions; // e.g. "After meals"
    private String doseTimes; // Comma-separated or JSON list of "HH:mm" strings, e.g. "08:00,14:00,20:00"
    private LocalDate startDate;
    private LocalDate endDate;
    private String doctorName;
    private String notes;
    private boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Prescription() {}

    public Prescription(int id, int userId, String medicineName, String medicineType, String dosageAmount,
                        String intakeInstructions, String doseTimes, LocalDate startDate, LocalDate endDate,
                        String doctorName, String notes) {
        this.id = id;
        this.userId = userId;
        this.medicineName = medicineName;
        this.medicineType = medicineType;
        this.dosageAmount = dosageAmount;
        this.intakeInstructions = intakeInstructions;
        this.doseTimes = doseTimes;
        this.startDate = startDate;
        this.endDate = endDate;
        this.doctorName = doctorName;
        this.notes = notes;
    }

    public List<String> getDoseTimeList() {
        if (doseTimes == null || doseTimes.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String cleaned = doseTimes.replace("[", "").replace("]", "").replace("\"", "").trim();
        String[] parts = cleaned.split(",");
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getMedicineType() { return medicineType; }
    public void setMedicineType(String medicineType) { this.medicineType = medicineType; }

    public String getDosageAmount() { return dosageAmount; }
    public void setDosageAmount(String dosageAmount) { this.dosageAmount = dosageAmount; }

    public String getIntakeInstructions() { return intakeInstructions; }
    public void setIntakeInstructions(String intakeInstructions) { this.intakeInstructions = intakeInstructions; }

    public String getDoseTimes() { return doseTimes; }
    public void setDoseTimes(String doseTimes) { this.doseTimes = doseTimes; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
