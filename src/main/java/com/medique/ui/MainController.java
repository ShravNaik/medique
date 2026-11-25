package com.medique.ui;

import com.medique.App;
import com.medique.dao.AdherenceLogDao;
import com.medique.dao.PrescriptionDao;
import com.medique.dao.UserDao;
import com.medique.model.AdherenceLog;
import com.medique.model.EmergencyContact;
import com.medique.model.Prescription;
import com.medique.model.User;
import com.medique.service.AdherenceAnalyticsService;
import com.medique.service.BackendSyncService;
import com.medique.service.ReminderSchedulerService;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainController {
    private final App app;
    private final User currentUser;
    private final UserDao userDao;
    private final PrescriptionDao prescriptionDao;
    private final AdherenceLogDao logDao;
    private final ReminderSchedulerService scheduler;
    private final AdherenceAnalyticsService analytics;
    private final BackendSyncService syncService;

    private BorderPane root;
    private StackPane contentArea;
    private VBox sidebar;

    private Button dashboardNavBtn;
    private Button prescriptionsNavBtn;
    private Button historyNavBtn;
    private Button caregiversNavBtn;
    private Button settingsNavBtn;

    public MainController(App app, User currentUser, UserDao userDao, PrescriptionDao prescriptionDao,
                          AdherenceLogDao logDao, ReminderSchedulerService scheduler,
                          AdherenceAnalyticsService analytics, BackendSyncService syncService) {
        this.app = app;
        this.currentUser = currentUser;
        this.userDao = userDao;
        this.prescriptionDao = prescriptionDao;
        this.logDao = logDao;
        this.scheduler = scheduler;
        this.analytics = analytics;
        this.syncService = syncService;

        buildUI();
    }

    public BorderPane getRoot() {
        return root;
    }

    private void buildUI() {
        root = new BorderPane();
        root.getStyleClass().add("theme-light");

        // Apply theme from user preference
        if ("DARK".equalsIgnoreCase(currentUser.getThemePreference())) {
            root.getStyleClass().remove("theme-light");
            root.getStyleClass().add("theme-dark");
        }
        if ("LARGE".equalsIgnoreCase(currentUser.getFontScale())) {
            root.getStyleClass().add("font-large");
        }

        // Sidebar - quiet, calibrated, no emojis
        sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        Label appTitle = new Label("Medique");
        appTitle.getStyleClass().add("sidebar-title");
        Label appSub = new Label("Adherence System");
        appSub.getStyleClass().add("sidebar-sub");
        VBox titleBox = new VBox(2, appTitle, appSub);
        titleBox.setPadding(new Insets(0, 0, 20, 4));

        dashboardNavBtn = createNavButton("Dashboard", true);
        prescriptionsNavBtn = createNavButton("Prescriptions", false);
        historyNavBtn = createNavButton("Adherence history", false);
        caregiversNavBtn = createNavButton("Caregivers", false);
        settingsNavBtn = createNavButton("Settings", false);

        dashboardNavBtn.setOnAction(e -> switchView("DASHBOARD"));
        prescriptionsNavBtn.setOnAction(e -> switchView("PRESCRIPTIONS"));
        historyNavBtn.setOnAction(e -> switchView("HISTORY"));
        caregiversNavBtn.setOnAction(e -> switchView("CAREGIVERS"));
        settingsNavBtn.setOnAction(e -> switchView("SETTINGS"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Sign out");
        logoutBtn.getStyleClass().add("sidebar-logout");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> app.showLoginScreen());

        sidebar.getChildren().addAll(
            titleBox, dashboardNavBtn, prescriptionsNavBtn, historyNavBtn,
            caregiversNavBtn, settingsNavBtn, spacer, logoutBtn
        );

        root.setLeft(sidebar);

        // Content Area
        contentArea = new StackPane();
        contentArea.setPadding(new Insets(28));
        root.setCenter(contentArea);

        switchView("DASHBOARD");
    }

    private Button createNavButton(String text, boolean active) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-btn");
        if (active) {
            btn.getStyleClass().add("active");
        }
        btn.setMaxWidth(Double.MAX_VALUE);
        return btn;
    }

    private void switchView(String viewName) {
        dashboardNavBtn.getStyleClass().remove("active");
        prescriptionsNavBtn.getStyleClass().remove("active");
        historyNavBtn.getStyleClass().remove("active");
        caregiversNavBtn.getStyleClass().remove("active");
        settingsNavBtn.getStyleClass().remove("active");

        contentArea.getChildren().clear();

        switch (viewName) {
            case "DASHBOARD":
                dashboardNavBtn.getStyleClass().add("active");
                contentArea.getChildren().add(buildDashboardView());
                break;
            case "PRESCRIPTIONS":
                prescriptionsNavBtn.getStyleClass().add("active");
                contentArea.getChildren().add(buildPrescriptionsView());
                break;
            case "HISTORY":
                historyNavBtn.getStyleClass().add("active");
                contentArea.getChildren().add(buildHistoryView());
                break;
            case "CAREGIVERS":
                caregiversNavBtn.getStyleClass().add("active");
                contentArea.getChildren().add(buildCaregiversView());
                break;
            case "SETTINGS":
                settingsNavBtn.getStyleClass().add("active");
                contentArea.getChildren().add(buildSettingsView());
                break;
        }
    }

    public void refreshData() {
        if (dashboardNavBtn.getStyleClass().contains("active")) {
            switchView("DASHBOARD");
        } else if (historyNavBtn.getStyleClass().contains("active")) {
            switchView("HISTORY");
        }
    }

    // --- VIEW BUILDERS ---

    private VBox buildDashboardView() {
        VBox view = new VBox(24);
        view.setAlignment(Pos.TOP_LEFT);

        VBox headerBox = new VBox(4);
        Label header = new Label("Medication schedule");
        header.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        Label dateLabel = new Label(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")));
        dateLabel.setStyle("-fx-text-fill: #71717A; -fx-font-size: 13px;");
        headerBox.getChildren().addAll(header, dateLabel);

        // Adherence Statistics Row
        HBox statsRow = new HBox(16);
        try {
            AdherenceAnalyticsService.AdherenceSummary todaySummary = analytics.getTodaySummary(currentUser.getId());
            AdherenceAnalyticsService.AdherenceSummary weekSummary = analytics.getLast7DaysSummary(currentUser.getId());

            statsRow.getChildren().addAll(
                createStatCard("Today's adherence", todaySummary.getFormattedPercentage(),
                    todaySummary.getTakenCount() + " of " + todaySummary.getTotalScheduled() + " doses logged today", "#0D9488"),
                createStatCard("7-day compliance", weekSummary.getFormattedPercentage(),
                    weekSummary.getTakenCount() + " of " + weekSummary.getTotalScheduled() + " doses this week", "#16A34A"),
                createStatCard("Missed doses", String.valueOf(todaySummary.getMissedCount()),
                    todaySummary.getMissedCount() == 0 ? "No unacknowledged doses" : "Requires caregiver attention",
                    todaySummary.getMissedCount() > 0 ? "#DC2626" : "#71717A")
            );
        } catch (SQLException e) {
            statsRow.getChildren().add(new Label("Could not load adherence statistics: " + e.getMessage()));
        }

        // Timeline of Doses
        Label sectionTitle = new Label("Today's schedule");
        sectionTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        VBox doseList = new VBox(8);
        try {
            List<Prescription> activePrescriptions = prescriptionDao.getActivePrescriptions(currentUser.getId());
            LocalDate today = LocalDate.now();
            List<AdherenceLog> todayLogs = logDao.getLogsForUser(
                    currentUser.getId(), today.atStartOfDay(), today.atTime(LocalTime.MAX));
            Set<String> takenDoseKeys = new HashSet<>();
            for (AdherenceLog log : todayLogs) {
                if ("TAKEN".equalsIgnoreCase(log.getStatus())) {
                    takenDoseKeys.add(log.getPrescriptionId() + "@" + log.getScheduledTime());
                }
            }

            if (activePrescriptions.isEmpty()) {
                VBox emptyBox = new VBox(12);
                emptyBox.setAlignment(Pos.CENTER);
                emptyBox.setPadding(new Insets(36));
                emptyBox.getStyleClass().add("card");
                Label emptyLabel = new Label("No active prescriptions scheduled for today");
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #71717A;");
                Button addBtn = new Button("Add prescription");
                addBtn.getStyleClass().add("btn-primary");
                addBtn.setOnAction(e -> switchView("PRESCRIPTIONS"));
                emptyBox.getChildren().addAll(emptyLabel, addBtn);
                doseList.getChildren().add(emptyBox);
            } else {
                for (Prescription p : activePrescriptions) {
                    if (today.isBefore(p.getStartDate()) || today.isAfter(p.getEndDate())) continue;

                    for (String timeStr : p.getDoseTimeList()) {
                        LocalDateTime parsedScheduledTime;
                        try {
                            parsedScheduledTime = LocalDateTime.of(
                                    today, LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm")));
                        } catch (DateTimeParseException e) {
                            parsedScheduledTime = null;
                        }
                        LocalDateTime scheduledTime = parsedScheduledTime;
                        boolean alreadyTaken = scheduledTime != null
                                && takenDoseKeys.contains(p.getId() + "@" + scheduledTime);

                        HBox doseRow = new HBox(16);
                        doseRow.setAlignment(Pos.CENTER_LEFT);
                        doseRow.getStyleClass().add("dose-row");

                        // Signature Detail: Live status indicator dot
                        Circle statusDot = new Circle(4);
                        statusDot.setFill(Color.web("#0D9488"));

                        Label timeLabel = new Label(timeStr);
                        timeLabel.getStyleClass().add("dose-time");
                        timeLabel.setPrefWidth(64);

                        VBox medInfo = new VBox(2);
                        Label nameLabel = new Label(p.getMedicineName() + " — " + p.getDosageAmount());
                        nameLabel.getStyleClass().add("dose-med-name");
                        nameLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
                        Label instrLabel = new Label(p.getIntakeInstructions() != null && !p.getIntakeInstructions().isEmpty()
                            ? p.getIntakeInstructions()
                            : p.getMedicineType());
                        instrLabel.setStyle("-fx-text-fill: #71717A; -fx-font-size: 13px;");
                        medInfo.getChildren().addAll(nameLabel, instrLabel);
                        HBox.setHgrow(medInfo, Priority.ALWAYS);

                        Button testTriggerBtn = new Button("Test alert");
                        testTriggerBtn.getStyleClass().add("btn-secondary");
                        testTriggerBtn.setOnAction(e -> {
                            scheduler.triggerReminder(p, LocalDateTime.now());
                        });

                        Button markTakenBtn = new Button(alreadyTaken ? "Taken"
                                : scheduledTime == null ? "Invalid time" : "Mark taken");
                        markTakenBtn.getStyleClass().add("btn-primary");
                        markTakenBtn.setDisable(alreadyTaken || scheduledTime == null);
                        if (!alreadyTaken && scheduledTime != null) {
                            markTakenBtn.setOnAction(e -> {
                                markTakenBtn.setText("Taken");
                                markTakenBtn.setDisable(true);
                                scheduler.recordActionTaken(p, scheduledTime, "Logged from schedule");
                                refreshData();
                            });
                        }

                        doseRow.getChildren().addAll(statusDot, timeLabel, medInfo, testTriggerBtn, markTakenBtn);
                        doseList.getChildren().add(doseRow);
                    }
                }
            }
        } catch (SQLException e) {
            doseList.getChildren().add(new Label("Database error: " + e.getMessage()));
        }

        ScrollPane scroll = new ScrollPane(doseList);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        view.getChildren().addAll(headerBox, statsRow, sectionTitle, scroll);
        return view;
    }

    private VBox createStatCard(String title, String mainValue, String subValue, String accentColor) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setPrefWidth(260);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("stat-label");

        Label valLbl = new Label(mainValue);
        valLbl.getStyleClass().add("stat-figure");
        if (accentColor != null) {
            valLbl.setStyle("-fx-text-fill: " + accentColor + ";");
        }

        Label subLbl = new Label(subValue);
        subLbl.getStyleClass().add("stat-sub");

        card.getChildren().addAll(titleLbl, valLbl, subLbl);
        return card;
    }

    private VBox buildPrescriptionsView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.TOP_LEFT);

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Prescriptions");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button addBtn = new Button("Add prescription");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> showPrescriptionEditorDialog(null));

        headerRow.getChildren().addAll(title, sp, addBtn);

        TableView<Prescription> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Prescription, String> nameCol = new TableColumn<>("Medicine");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicineName()));

        TableColumn<Prescription, String> typeCol = new TableColumn<>("Form");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicineType()));

        TableColumn<Prescription, String> dosageCol = new TableColumn<>("Dosage");
        dosageCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDosageAmount()));

        TableColumn<Prescription, String> timesCol = new TableColumn<>("Schedule");
        timesCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDoseTimes()));

        TableColumn<Prescription, String> datesCol = new TableColumn<>("Course duration");
        datesCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStartDate() + " to " + c.getValue().getEndDate()));

        TableColumn<Prescription, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Archived"));

        table.getColumns().addAll(nameCol, typeCol, dosageCol, timesCol, datesCol, statusCol);

        try {
            List<Prescription> list = prescriptionDao.getAllPrescriptions(currentUser.getId());
            table.getItems().addAll(list);
        } catch (SQLException ex) {
            System.err.println("Error loading prescriptions table: " + ex.getMessage());
        }

        HBox actionsRow = new HBox(12);
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> {
            Prescription selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showPrescriptionEditorDialog(selected);
            }
        });

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setOnAction(e -> {
            Prescription selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete prescription for " + selected.getMedicineName() + "?", ButtonType.YES, ButtonType.NO);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        try {
                            prescriptionDao.deletePrescription(selected.getId(), currentUser.getId());
                            switchView("PRESCRIPTIONS");
                        } catch (SQLException ex) {
                            System.err.println("Error deleting prescription: " + ex.getMessage());
                        }
                    }
                });
            }
        });

        actionsRow.getChildren().addAll(editBtn, deleteBtn);
        view.getChildren().addAll(headerRow, table, actionsRow);
        return view;
    }

    private void showPrescriptionEditorDialog(Prescription existing) {
        Dialog<Prescription> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add prescription" : "Edit prescription");

        VBox form = new VBox(12);
        form.setPadding(new Insets(24));
        form.setPrefWidth(440);

        TextField nameField = new TextField(existing != null ? existing.getMedicineName() : "");
        nameField.setPromptText("e.g. Metformin");

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Tablet", "Syrup", "Injection", "Capsule", "Drops", "Inhaler");
        typeCombo.setValue(existing != null ? existing.getMedicineType() : "Tablet");

        TextField dosageField = new TextField(existing != null ? existing.getDosageAmount() : "");
        dosageField.setPromptText("e.g. 500 mg");

        TextField instructionsField = new TextField(existing != null ? existing.getIntakeInstructions() : "");
        instructionsField.setPromptText("e.g. After meals with water");

        TextField timesField = new TextField(existing != null ? existing.getDoseTimes() : "08:00, 20:00");
        timesField.setPromptText("Comma-separated 24h format (e.g. 08:00, 20:00)");

        DatePicker startDatePicker = new DatePicker(existing != null ? existing.getStartDate() : LocalDate.now());
        DatePicker endDatePicker = new DatePicker(existing != null ? existing.getEndDate() : LocalDate.now().plusMonths(1));

        TextField doctorField = new TextField(existing != null ? existing.getDoctorName() : "");
        doctorField.setPromptText("Physician name (optional)");

        form.getChildren().addAll(
            new Label("Medicine name"), nameField,
            new Label("Type"), typeCombo,
            new Label("Dosage"), dosageField,
            new Label("Intake instructions"), instructionsField,
            new Label("Dose times (HH:mm)"), timesField,
            new Label("Start date"), startDatePicker,
            new Label("End date"), endDatePicker,
            new Label("Doctor name"), doctorField
        );

        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                if (nameField.getText().trim().isEmpty() || dosageField.getText().trim().isEmpty()) {
                    return null;
                }
                Prescription p = existing != null ? existing : new Prescription();
                p.setUserId(currentUser.getId());
                p.setMedicineName(nameField.getText().trim());
                p.setMedicineType(typeCombo.getValue());
                p.setDosageAmount(dosageField.getText().trim());
                p.setIntakeInstructions(instructionsField.getText().trim());
                p.setDoseTimes(timesField.getText().trim());
                p.setStartDate(startDatePicker.getValue());
                p.setEndDate(endDatePicker.getValue());
                p.setDoctorName(doctorField.getText().trim());
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                if (existing == null) {
                    prescriptionDao.createPrescription(p);
                } else {
                    prescriptionDao.updatePrescription(p);
                }
                switchView("PRESCRIPTIONS");
            } catch (SQLException ex) {
                System.err.println("Failed to save prescription: " + ex.getMessage());
            }
        });
    }

    private VBox buildHistoryView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Adherence history");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        TableView<AdherenceLog> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<AdherenceLog, String> dateCol = new TableColumn<>("Scheduled time");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getScheduledTime() != null ? c.getValue().getScheduledTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : ""
        ));

        TableColumn<AdherenceLog, String> medCol = new TableColumn<>("Medicine & dosage");
        medCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMedicineName() + " (" + c.getValue().getDosageAmount() + ")"));

        TableColumn<AdherenceLog, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus()));

        TableColumn<AdherenceLog, String> actCol = new TableColumn<>("Logged at");
        actCol.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getActionTime() != null ? c.getValue().getActionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "-"
        ));

        TableColumn<AdherenceLog, String> notesCol = new TableColumn<>("Notes");
        notesCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes() != null ? c.getValue().getNotes() : ""));

        table.getColumns().addAll(dateCol, medCol, statusCol, actCol, notesCol);

        try {
            List<AdherenceLog> logs = logDao.getAllLogsForUser(currentUser.getId());
            table.getItems().addAll(logs);
        } catch (SQLException ex) {
            System.err.println("Error loading history: " + ex.getMessage());
        }

        view.getChildren().addAll(title, table);
        return view;
    }

    private VBox buildCaregiversView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Caregiver escalation");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        Label desc = new Label("If a dose remains unacknowledged for more than 30 minutes, an automated alert is sent to your primary caregiver.");
        desc.setStyle("-fx-text-fill: #71717A; -fx-font-size: 14px;");

        VBox formCard = new VBox(12);
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(520);

        Label cardTitle = new Label("Primary emergency contact");
        cardTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        TextField nameField = new TextField();
        nameField.setPromptText("Caregiver name");

        TextField relField = new TextField();
        relField.setPromptText("e.g. Daughter, Spouse, Primary Physician");

        TextField emailField = new TextField();
        emailField.setPromptText("caregiver@example.com");

        TextField phoneField = new TextField();
        phoneField.setPromptText("Phone number");

        Label statusLbl = new Label();

        try {
            List<EmergencyContact> contacts = userDao.getEmergencyContacts(currentUser.getId());
            if (!contacts.isEmpty()) {
                EmergencyContact c = contacts.get(0);
                nameField.setText(c.getContactName());
                relField.setText(c.getRelationship());
                emailField.setText(c.getEmail());
                phoneField.setText(c.getPhone());
            }
        } catch (SQLException ignored) {}

        Button saveBtn = new Button("Save contact");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            if (name.isEmpty() || email.isEmpty()) {
                statusLbl.setText("Name and email are required.");
                statusLbl.setStyle("-fx-text-fill: #DC2626;");
                return;
            }
            try {
                EmergencyContact contact = new EmergencyContact(0, currentUser.getId(), name, relField.getText().trim(),
                        email, phoneField.getText().trim(), true);
                userDao.saveEmergencyContact(contact);
                statusLbl.setText("Contact saved.");
                statusLbl.setStyle("-fx-text-fill: #16A34A;");
            } catch (SQLException ex) {
                statusLbl.setText("Error: " + ex.getMessage());
                statusLbl.setStyle("-fx-text-fill: #DC2626;");
            }
        });

        formCard.getChildren().addAll(cardTitle, new Label("Name"), nameField,
                new Label("Relationship"), relField, new Label("Email address"),
                emailField, new Label("Phone"), phoneField, statusLbl, saveBtn);

        view.getChildren().addAll(title, desc, formCard);
        return view;
    }

    private VBox buildSettingsView() {
        VBox view = new VBox(20);
        view.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Preferences & synchronization");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));

        VBox themeCard = new VBox(12);
        themeCard.getStyleClass().add("card");
        themeCard.setMaxWidth(480);

        Label themeTitle = new Label("Display & accessibility");
        themeTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        CheckBox darkThemeCheck = new CheckBox("Dark theme");
        darkThemeCheck.setSelected("DARK".equalsIgnoreCase(currentUser.getThemePreference()));
        darkThemeCheck.setOnAction(e -> {
            String newPref = darkThemeCheck.isSelected() ? "DARK" : "LIGHT";
            currentUser.setThemePreference(newPref);
            try {
                userDao.updateUserPreferences(currentUser.getId(), newPref, currentUser.getFontScale());
            } catch (SQLException ignored) {}
            if (darkThemeCheck.isSelected()) {
                root.getStyleClass().remove("theme-light");
                root.getStyleClass().add("theme-dark");
            } else {
                root.getStyleClass().remove("theme-dark");
                root.getStyleClass().add("theme-light");
            }
        });

        CheckBox largeFontCheck = new CheckBox("High-visibility large typography");
        largeFontCheck.setSelected("LARGE".equalsIgnoreCase(currentUser.getFontScale()));
        largeFontCheck.setOnAction(e -> {
            String newFont = largeFontCheck.isSelected() ? "LARGE" : "NORMAL";
            currentUser.setFontScale(newFont);
            try {
                userDao.updateUserPreferences(currentUser.getId(), currentUser.getThemePreference(), newFont);
            } catch (SQLException ignored) {}
            if (largeFontCheck.isSelected()) {
                root.getStyleClass().add("font-large");
            } else {
                root.getStyleClass().remove("font-large");
            }
        });

        themeCard.getChildren().addAll(themeTitle, darkThemeCheck, largeFontCheck);

        // Backend Sync Card
        VBox syncCard = new VBox(12);
        syncCard.getStyleClass().add("card");
        syncCard.setMaxWidth(480);

        Label syncTitle = new Label("Cloud synchronization");
        syncTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));

        Label syncStatus = new Label("Pending offline records: " + syncService.getPendingQueueCount());
        syncStatus.setStyle("-fx-text-fill: #71717A;");

        Button manualSyncBtn = new Button("Synchronize now");
        manualSyncBtn.getStyleClass().add("btn-primary");
        manualSyncBtn.setOnAction(e -> {
            manualSyncBtn.setDisable(true);
            syncStatus.setText("Connecting to cloud...");
            syncService.syncWithBackendAsync(currentUser.getId()).thenAccept(success -> {
                Platform.runLater(() -> {
                    manualSyncBtn.setDisable(false);
                    if (success) {
                        syncStatus.setText("Synchronization complete.");
                        syncStatus.setStyle("-fx-text-fill: #16A34A;");
                    } else {
                        syncStatus.setText("Server offline. Queued locally.");
                        syncStatus.setStyle("-fx-text-fill: #D97706;");
                    }
                });
            });
        });

        syncCard.getChildren().addAll(syncTitle, syncStatus, manualSyncBtn);

        view.getChildren().addAll(title, themeCard, syncCard);
        return view;
    }
}
