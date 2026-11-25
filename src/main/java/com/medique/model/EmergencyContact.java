package com.medique.model;

import java.time.LocalDateTime;

public class EmergencyContact {
    private int id;
    private int userId;
    private String contactName;
    private String relationship;
    private String email;
    private String phone;
    private boolean isPrimary = true;
    private LocalDateTime createdAt;

    public EmergencyContact() {}

    public EmergencyContact(int id, int userId, String contactName, String relationship, String email, String phone, boolean isPrimary) {
        this.id = id;
        this.userId = userId;
        this.contactName = contactName;
        this.relationship = relationship;
        this.email = email;
        this.phone = phone;
        this.isPrimary = isPrimary;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
