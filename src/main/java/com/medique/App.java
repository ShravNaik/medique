package com.medique;

import com.medique.dao.AdherenceLogDao;
import com.medique.dao.DatabaseManager;
import com.medique.dao.PrescriptionDao;
import com.medique.dao.UserDao;
import com.medique.model.User;
import com.medique.service.AdherenceAnalyticsService;
import com.medique.service.BackendSyncService;
import com.medique.service.CaregiverNotificationService;
import com.medique.service.ReminderSchedulerService;
import com.medique.ui.MainController;
import com.medique.ui.ReminderAlertStage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;

public class App extends Application {
    private static App instance;
    private Stage primaryStage;
    private TrayIcon trayIcon;

    private UserDao userDao;
    private PrescriptionDao prescriptionDao;
    private AdherenceLogDao adherenceLogDao;

    private CaregiverNotificationService caregiverService;
    private ReminderSchedulerService schedulerService;
    private AdherenceAnalyticsService analyticsService;
    private BackendSyncService syncService;

    private User currentUser;
    private MainController mainController;

    public static App getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) {
        instance = this;
        this.primaryStage = stage;
        setWindowIcons();

        // Initialize SQLite Database schema
        DatabaseManager.initializeDatabase();

        // Initialize Repositories and Services
        this.userDao = new UserDao();
        this.prescriptionDao = new PrescriptionDao();
        this.adherenceLogDao = new AdherenceLogDao();
        this.caregiverService = new CaregiverNotificationService();
        this.schedulerService = new ReminderSchedulerService(prescriptionDao, adherenceLogDao, userDao, caregiverService);
        this.analyticsService = new AdherenceAnalyticsService(adherenceLogDao);
        this.syncService = new BackendSyncService();

        // Register reminder listener to trigger popup alert on UI thread
        schedulerService.addListener(new ReminderSchedulerService.ReminderListener() {
            @Override
            public void onDoseReminder(com.medique.model.Prescription prescription, java.time.LocalDateTime scheduledTime) {
                Platform.runLater(() -> {
                    ReminderAlertStage.showDoseAlert(prescription, scheduledTime, schedulerService);
                    if (trayIcon != null) {
                        trayIcon.displayMessage(
                            "Medique dose reminder",
                            "Scheduled dose for " + prescription.getMedicineName() + " (" + prescription.getDosageAmount() + ")",
                            TrayIcon.MessageType.INFO
                        );
                    }
                });
            }

            @Override
            public void onReminderDismissed(int prescriptionId, String action) {
                Platform.runLater(() -> {
                    if (mainController != null) {
                        mainController.refreshData();
                    }
                });
            }
        });

        // Setup System Tray
        setupSystemTray();

        // Ensure window closes to tray instead of terminating
        Platform.setImplicitExit(false);
        primaryStage.setOnCloseRequest(event -> {
            event.consume();
            primaryStage.hide();
            if (trayIcon != null) {
                trayIcon.displayMessage(
                    "Medique is running in background",
                    "Dose reminders continue to monitor your schedule.",
                    TrayIcon.MessageType.INFO
                );
            }
        });

        showLoginScreen();
    }

    private void setWindowIcons() {
        for (int size : new int[]{16, 24, 32, 48, 64}) {
            String resourcePath = "/MediqueIcon-" + size + ".png";
            URL iconResource = App.class.getResource(resourcePath);
            if (iconResource == null) {
                throw new IllegalStateException("Application icon not found: " + resourcePath);
            }

            Image icon = new Image(iconResource.toExternalForm());
            if (icon.isError()) {
                throw new IllegalStateException("Failed to load application icon: " + resourcePath, icon.getException());
            }
            primaryStage.getIcons().add(icon);
        }
    }

    public void showLoginScreen() {
        VBox root = new VBox(24);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().addAll("theme-light");

        Label title = new Label("Medique");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.setStyle("-fx-text-fill: #0D9488;");

        Label subtitle = new Label("Prescription scheduling & adherence tracking");
        subtitle.setStyle("-fx-text-fill: #71717A; -fx-font-size: 14px;");

        VBox formCard = new VBox(14);
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(360);

        Label formTitle = new Label("Sign in");
        formTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 13px;");

        Button loginBtn = new Button("Sign in");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Button registerBtn = new Button("Create patient account");
        registerBtn.getStyleClass().add("btn-secondary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        loginBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText();
            try {
                Optional<User> userOpt = userDao.findByUsername(u);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    if (com.medique.util.PasswordHasher.verifyPassword(p, user.getSalt(), user.getPasswordHash())) {
                        setCurrentUser(user);
                        showMainDashboard();
                        return;
                    }
                }
                errorLabel.setText("Invalid username or password.");
            } catch (SQLException ex) {
                errorLabel.setText("Database error: " + ex.getMessage());
            }
        });

        registerBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText();
            if (u.isEmpty() || p.isEmpty()) {
                errorLabel.setText("Username and password are required.");
                return;
            }
            try {
                String salt = com.medique.util.PasswordHasher.generateSalt();
                String hash = com.medique.util.PasswordHasher.hashPassword(p, salt);
                User newUser = new User(0, u, hash, salt, "Patient Profile", u + "@example.com", "");
                userDao.createUser(newUser);
                setCurrentUser(newUser);
                showMainDashboard();
            } catch (SQLException ex) {
                errorLabel.setText("Registration error: " + ex.getMessage());
            }
        });

        formCard.getChildren().addAll(formTitle, usernameField, passwordField, errorLabel, loginBtn, registerBtn);
        root.getChildren().addAll(title, subtitle, formCard);

        Scene scene = new Scene(root, 880, 620);
        scene.getStylesheets().add(getClass().getResource("/com/medique/css/styles.css").toExternalForm());
        primaryStage.setTitle("Medique — Sign in");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void showMainDashboard() {
        this.mainController = new MainController(this, currentUser, userDao, prescriptionDao,
                adherenceLogDao, schedulerService, analyticsService, syncService);
        Scene scene = new Scene(mainController.getRoot(), 1080, 700);
        scene.getStylesheets().add(getClass().getResource("/com/medique/css/styles.css").toExternalForm());
        primaryStage.setTitle("Medique — " + currentUser.getFullName());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        schedulerService.start(user.getId());
    }

    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();

            // Create a calibrated 16x16 teal medical icon for tray
            int w = 16, h = 16;
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setColor(new Color(13, 148, 136));
            g.fillOval(0, 0, w, h);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2));
            g.drawLine(8, 4, 8, 12);
            g.drawLine(4, 8, 12, 8);
            g.dispose();

            PopupMenu popup = new PopupMenu();
            MenuItem openItem = new MenuItem("Open Medique");
            openItem.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                schedulerService.stop();
                Platform.exit();
                System.exit(0);
            });

            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon = new TrayIcon(image, "Medique Medicine Reminder", popup);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(() -> {
                primaryStage.show();
                primaryStage.toFront();
            }));

            tray.add(trayIcon);
        } catch (Exception e) {
            System.err.println("Could not initialize system tray: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
