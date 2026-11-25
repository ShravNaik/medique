package com.medique.service;

import com.medique.dao.AdherenceLogDao;
import com.medique.dao.PrescriptionDao;
import com.medique.dao.UserDao;
import com.medique.model.AdherenceLog;
import com.medique.model.EmergencyContact;
import com.medique.model.Prescription;
import com.medique.model.User;
import com.medique.util.SoundPlayer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public class ReminderSchedulerService {

    public interface ReminderListener {
        void onDoseReminder(Prescription prescription, LocalDateTime scheduledTime);
        void onReminderDismissed(int prescriptionId, String action);
    }

    private final PrescriptionDao prescriptionDao;
    private final AdherenceLogDao logDao;
    private final UserDao userDao;
    private final CaregiverNotificationService caregiverService;

    private ScheduledExecutorService scheduler;
    private final Set<ReminderListener> listeners = new CopyOnWriteArraySet<>();
    private final Map<String, Future<?>> activeReminders = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> unacknowledgedDoses = new ConcurrentHashMap<>();

    private int currentUserId;

    public ReminderSchedulerService(PrescriptionDao prescriptionDao, AdherenceLogDao logDao,
                                    UserDao userDao, CaregiverNotificationService caregiverService) {
        this.prescriptionDao = prescriptionDao;
        this.logDao = logDao;
        this.userDao = userDao;
        this.caregiverService = caregiverService;
    }

    public void addListener(ReminderListener listener) {
        listeners.add(listener);
    }

    public void removeListener(ReminderListener listener) {
        listeners.remove(listener);
    }

    public synchronized void start(int userId) {
        this.currentUserId = userId;
        stop();
        scheduler = Executors.newScheduledThreadPool(2);

        // Schedule periodic evaluation every 30 seconds
        scheduler.scheduleAtFixedRate(this::evaluateReminders, 0, 30, TimeUnit.SECONDS);

        // Schedule escalation watchdog every 60 seconds
        scheduler.scheduleAtFixedRate(this::checkEscalationWatchdog, 10, 60, TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        activeReminders.clear();
        unacknowledgedDoses.clear();
    }

    public void evaluateReminders() {
        if (currentUserId <= 0) return;

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        try {
            List<Prescription> prescriptions = prescriptionDao.getActivePrescriptions(currentUserId);
            for (Prescription p : prescriptions) {
                if (today.isBefore(p.getStartDate()) || today.isAfter(p.getEndDate())) {
                    continue;
                }

                for (String doseTimeStr : p.getDoseTimeList()) {
                    try {
                        LocalTime doseTime = LocalTime.parse(doseTimeStr, DateTimeFormatter.ofPattern("HH:mm"));
                        LocalDateTime scheduled = LocalDateTime.of(today, doseTime);

                        // Within 30 seconds of scheduled dose time
                        long diffSeconds = java.time.Duration.between(now, doseTime).toSeconds();
                        String key = p.getId() + "@" + scheduled;

                        if (Math.abs(diffSeconds) <= 30 && !activeReminders.containsKey(key)) {
                            // Check if already logged for today
                            if (!isAlreadyLogged(p.getId(), scheduled)) {
                                triggerReminder(p, scheduled);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            System.err.println("Error evaluating reminders: " + e.getMessage());
        }
    }

    private boolean isAlreadyLogged(int prescriptionId, LocalDateTime scheduled) {
        try {
            List<AdherenceLog> logs = logDao.getLogsForUser(currentUserId, scheduled.minusMinutes(5), scheduled.plusMinutes(5));
            for (AdherenceLog l : logs) {
                if (l.getPrescriptionId() == prescriptionId && !l.getStatus().equalsIgnoreCase("SNOOZED")) {
                    return true;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void triggerReminder(Prescription prescription, LocalDateTime scheduledTime) {
        String key = prescription.getId() + "@" + scheduledTime;
        activeReminders.put(key, CompletableFuture.completedFuture(null));
        unacknowledgedDoses.put(key, LocalDateTime.now());

        SoundPlayer.playReminderChime();

        for (ReminderListener listener : listeners) {
            listener.onDoseReminder(prescription, scheduledTime);
        }
    }

    public void recordActionTaken(Prescription prescription, LocalDateTime scheduledTime, String notes) {
        String key = prescription.getId() + "@" + scheduledTime;
        unacknowledgedDoses.remove(key);

        try {
            AdherenceLog log = new AdherenceLog(0, prescription.getId(), currentUserId, scheduledTime,
                    LocalDateTime.now(), "TAKEN", notes);
            logDao.logDoseAction(log);
        } catch (Exception e) {
            System.err.println("Failed to log dose taken: " + e.getMessage());
        }

        for (ReminderListener listener : listeners) {
            listener.onReminderDismissed(prescription.getId(), "TAKEN");
        }
    }

    public void recordActionSkipped(Prescription prescription, LocalDateTime scheduledTime, String reason) {
        String key = prescription.getId() + "@" + scheduledTime;
        unacknowledgedDoses.remove(key);

        try {
            AdherenceLog log = new AdherenceLog(0, prescription.getId(), currentUserId, scheduledTime,
                    LocalDateTime.now(), "SKIPPED", reason);
            logDao.logDoseAction(log);
        } catch (Exception e) {
            System.err.println("Failed to log dose skipped: " + e.getMessage());
        }

        for (ReminderListener listener : listeners) {
            listener.onReminderDismissed(prescription.getId(), "SKIPPED");
        }
    }

    public void recordActionSnooze(Prescription prescription, LocalDateTime scheduledTime, int snoozeMinutes) {
        String key = prescription.getId() + "@" + scheduledTime;

        try {
            AdherenceLog log = new AdherenceLog(0, prescription.getId(), currentUserId, scheduledTime,
                    LocalDateTime.now(), "SNOOZED", "Snoozed for " + snoozeMinutes + " mins");
            logDao.logDoseAction(log);
        } catch (Exception ignored) {}

        if (scheduler != null && !scheduler.isShutdown()) {
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                triggerReminder(prescription, scheduledTime);
            }, snoozeMinutes, TimeUnit.MINUTES);
            activeReminders.put(key, future);
        }

        for (ReminderListener listener : listeners) {
            listener.onReminderDismissed(prescription.getId(), "SNOOZED");
        }
    }

    private void checkEscalationWatchdog() {
        if (currentUserId <= 0) return;

        LocalDateTime now = LocalDateTime.now();
        List<String> escalatedKeys = new ArrayList<>();

        for (Map.Entry<String, LocalDateTime> entry : unacknowledgedDoses.entrySet()) {
            String key = entry.getKey();
            LocalDateTime triggeredAt = entry.getValue();

            // 30 minute timeout as per REQ-2 in SRS
            long elapsedMinutes = java.time.Duration.between(triggeredAt, now).toMinutes();
            if (elapsedMinutes >= 30) {
                escalatedKeys.add(key);
                executeCaregiverEscalation(key);
            }
        }

        for (String key : escalatedKeys) {
            unacknowledgedDoses.remove(key);
        }
    }

    private void executeCaregiverEscalation(String key) {
        try {
            String[] parts = key.split("@");
            int prescriptionId = Integer.parseInt(parts[0]);
            LocalDateTime scheduled = LocalDateTime.parse(parts[1]);

            Optional<Prescription> optP = prescriptionDao.findById(prescriptionId);
            Optional<User> optUser = userDao.findById(currentUserId);
            List<EmergencyContact> contacts = userDao.getEmergencyContacts(currentUserId);

            if (optP.isPresent() && optUser.isPresent() && !contacts.isEmpty()) {
                Prescription p = optP.get();
                User user = optUser.get();
                EmergencyContact primaryCaregiver = contacts.get(0);

                // Log missed dose
                AdherenceLog log = new AdherenceLog(0, p.getId(), currentUserId, scheduled,
                        LocalDateTime.now(), "MISSED", "Unacknowledged past 30m escalation threshold");
                logDao.logDoseAction(log);

                // Dispatch alert
                caregiverService.sendMissedDoseAlertAsync(user.getFullName(), primaryCaregiver, p, scheduled);
            }
        } catch (Exception e) {
            System.err.println("Failed to escalate missed dose: " + e.getMessage());
        }
    }
}
