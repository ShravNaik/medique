package com.medique.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.medique.dao.AdherenceLogDao;
import com.medique.dao.DatabaseManager;
import com.medique.dao.PrescriptionDao;
import com.medique.model.AdherenceLog;
import com.medique.model.Prescription;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class BackendSyncService {
    private final PrescriptionDao prescriptionDao;
    private final AdherenceLogDao logDao;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private String backendUrl = "https://api.medique.local/v1/sync";

    public BackendSyncService() {
        this.prescriptionDao = new PrescriptionDao();
        this.logDao = new AdherenceLogDao();
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void setBackendUrl(String url) {
        this.backendUrl = url;
    }

    public String exportSyncPayloadJson(int userId) throws Exception {
        List<Prescription> prescriptions = prescriptionDao.getAllPrescriptions(userId);
        List<AdherenceLog> logs = logDao.getAllLogsForUser(userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("exportedAt", java.time.LocalDateTime.now().toString());
        payload.put("prescriptions", prescriptions);
        payload.put("adherenceLogs", logs);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    public CompletableFuture<Boolean> syncWithBackendAsync(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String payloadJson = exportSyncPayloadJson(userId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(backendUrl))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payloadJson))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    clearSyncQueue(userId);
                    return true;
                } else {
                    queueSyncLocally("FULL_SYNC", userId, payloadJson);
                    return false;
                }
            } catch (Exception e) {
                try {
                    queueSyncLocally("FULL_SYNC", userId, exportSyncPayloadJson(userId));
                } catch (Exception ignored) {}
                return false;
            }
        });
    }

    public void queueSyncLocally(String entityType, int entityId, String payloadJson) {
        String sql = "INSERT INTO sync_queue (entity_type, entity_id, operation, payload_json) VALUES (?, ?, 'SYNC', ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, entityType);
            pstmt.setInt(2, entityId);
            pstmt.setString(3, payloadJson);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to record local sync queue: " + e.getMessage());
        }
    }

    public int getPendingQueueCount() {
        String sql = "SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error reading sync queue: " + e.getMessage());
        }
        return 0;
    }

    private void clearSyncQueue(int userId) {
        String sql = "DELETE FROM sync_queue WHERE entity_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {}
    }
}
