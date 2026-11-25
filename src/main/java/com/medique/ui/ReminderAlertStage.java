package com.medique.ui;

import com.medique.model.Prescription;
import com.medique.service.ReminderSchedulerService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReminderAlertStage {

    public static void showDoseAlert(Prescription p, LocalDateTime scheduledTime, ReminderSchedulerService scheduler) {
        Stage alertStage = new Stage();
        alertStage.initStyle(StageStyle.UTILITY);
        alertStage.setAlwaysOnTop(true);
        alertStage.setTitle("Dose reminder — " + p.getMedicineName());

        VBox root = new VBox(16);
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(28));
        root.getStyleClass().addAll("alert-popup-root");

        Label categoryTag = new Label("Scheduled dose reminder");
        categoryTag.getStyleClass().add("alert-category-tag");

        VBox titleBlock = new VBox(4);
        Label medName = new Label(p.getMedicineName());
        medName.getStyleClass().add("alert-med-name");

        Label dosage = new Label(p.getDosageAmount() + " (" + p.getMedicineType() + ")");
        dosage.getStyleClass().add("alert-dose-text");
        titleBlock.getChildren().addAll(medName, dosage);

        String instructions = p.getIntakeInstructions() != null && !p.getIntakeInstructions().isEmpty()
                ? p.getIntakeInstructions()
                : "Scheduled for " + scheduledTime.format(DateTimeFormatter.ofPattern("hh:mm a"));
        Label instLabel = new Label(instructions);
        instLabel.getStyleClass().add("alert-instructions");

        // Action Buttons: One prominent primary action, secondary snooze, tertiary skip
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(12, 0, 0, 0));

        Button takenBtn = new Button("Mark taken");
        takenBtn.getStyleClass().add("btn-primary");
        takenBtn.setStyle("-fx-font-size: 15px; -fx-padding: 10px 22px;");

        Button snoozeBtn = new Button("Snooze 10 min");
        snoozeBtn.getStyleClass().add("btn-secondary");
        snoozeBtn.setStyle("-fx-font-size: 15px; -fx-padding: 10px 18px;");

        Button skipBtn = new Button("Skip dose");
        skipBtn.getStyleClass().add("btn-secondary");
        skipBtn.setStyle("-fx-font-size: 15px; -fx-padding: 10px 16px; -fx-text-fill: #DC2626; -fx-border-color: #FECACA;");

        takenBtn.setOnAction(e -> {
            scheduler.recordActionTaken(p, scheduledTime, "Confirmed on reminder");
            alertStage.close();
        });

        snoozeBtn.setOnAction(e -> {
            scheduler.recordActionSnooze(p, scheduledTime, 10);
            alertStage.close();
        });

        skipBtn.setOnAction(e -> {
            scheduler.recordActionSkipped(p, scheduledTime, "Skipped on reminder");
            alertStage.close();
        });

        buttonBox.getChildren().addAll(takenBtn, snoozeBtn, skipBtn);
        root.getChildren().addAll(categoryTag, titleBlock, instLabel, buttonBox);

        Scene scene = new Scene(root, 540, 270);
        scene.getStylesheets().add(ReminderAlertStage.class.getResource("/com/medique/css/styles.css").toExternalForm());
        alertStage.setScene(scene);
        alertStage.show();
    }
}
