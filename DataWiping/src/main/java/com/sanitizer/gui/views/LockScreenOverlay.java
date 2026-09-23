package com.sanitizer.gui.views;

import com.sanitizer.a11y.AccessibilityManager;
import com.sanitizer.gui.navigation.NavigationManager;
import com.sanitizer.session.SessionAutoLockManager;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * LockScreenOverlay — Security-compliant lock screen overlay displaying authenticated officer
 * credentials and requiring PIN/Password re-entry to restore session state.
 */
public class LockScreenOverlay extends StackPane {

    private final NavigationManager navManager;
    private final PasswordField txtPin = new PasswordField();
    private final Label lblError = new Label();

    public LockScreenOverlay(NavigationManager navManager) {
        this.navManager = navManager;
        buildUi();
    }

    private void buildUi() {
        this.setStyle("-fx-background-color: rgba(15, 23, 42, 0.94);");
        this.setAlignment(Pos.CENTER);

        VBox card = new VBox(20);
        card.setMaxWidth(440);
        card.setPadding(new Insets(32));
        card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 16px; " +
                "-fx-border-color: #334155; -fx-border-radius: 16px; -fx-border-width: 1.5px; " +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 24, 0, 0, 8);");
        card.setAlignment(Pos.CENTER);

        // Lock Icon Header
        Label lockIcon = new Label("🔒");
        lockIcon.setStyle("-fx-font-size: 38px; -fx-background-color: rgba(239,68,68,0.15); " +
                "-fx-background-radius: 50%; -fx-padding: 12 18;");

        Label lblTitle = new Label("SESSION AUTO-LOCKED");
        lblTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC; -fx-letter-spacing: 0.5px;");

        Label lblCompliance = new Label("FISMA / HIPAA / NIST SP 800-53 Inactivity Security Standard");
        lblCompliance.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F59E0B; -fx-background-color: rgba(245,158,11,0.12); -fx-padding: 4 10; -fx-background-radius: 6px;");

        // Officer Credential Box
        VBox officerInfo = new VBox(6);
        officerInfo.setStyle("-fx-background-color: #0F172A; -fx-padding: 14; -fx-background-radius: 10px; -fx-border-color: #334155; -fx-border-radius: 10px;");
        officerInfo.setAlignment(Pos.CENTER_LEFT);

        Label lblOfficer = new Label("👤 " + navManager.getOfficerName());
        lblOfficer.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        HBox roleRow = new HBox(8);
        roleRow.setAlignment(Pos.CENTER_LEFT);
        Label lblRole = new Label(navManager.getRole());
        lblRole.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");
        Label lblAgency = new Label("•  Agency: " + navManager.getAgencyId());
        lblAgency.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        roleRow.getChildren().addAll(lblRole, lblAgency);

        String lockedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss yyyy-MM-dd"));
        Label lblTime = new Label("Locked at: " + lockedTime);
        lblTime.setStyle("-fx-font-size: 10px; -fx-text-fill: #64748B;");

        officerInfo.getChildren().addAll(lblOfficer, roleRow, lblTime);

        // PIN Input & Unlock Form
        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER_LEFT);

        Label lblPinPrompt = new Label("Security PIN / Passcode:");
        lblPinPrompt.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #E2E8F0;");

        txtPin.setPromptText("Enter PIN to resume (e.g. 1234)");
        txtPin.setStyle("-fx-font-size: 14px; -fx-padding: 10 14;");
        txtPin.setMaxWidth(Double.MAX_VALUE);
        txtPin.setOnAction(e -> handleUnlock());
        AccessibilityManager.setupAccessible(txtPin, "Session Unlock PIN", "Enter PIN to resume session", AccessibleRole.PASSWORD_FIELD);

        lblError.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
        lblError.setVisible(false);

        form.getChildren().addAll(lblPinPrompt, txtPin, lblError);

        // Actions
        Button btnUnlock = new Button("🔓 Resume Session");
        btnUnlock.getStyleClass().add("button-primary");
        btnUnlock.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 12 20;");
        btnUnlock.setMaxWidth(Double.MAX_VALUE);
        btnUnlock.setDefaultButton(true);
        btnUnlock.setOnAction(e -> handleUnlock());
        AccessibilityManager.setupAccessible(btnUnlock, "Unlock Session", "Submit PIN to resume session", AccessibleRole.BUTTON);

        Button btnSignOut = new Button("🚪 Sign Out / Switch Officer");
        btnSignOut.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-size: 12px; -fx-cursor: hand;");
        btnSignOut.setOnAction(e -> {
            SessionAutoLockManager.getInstance().unlockSession("1234", navManager.getOfficerName(), navManager.getAgencyId(), navManager.getRole());
            navManager.logout();
        });

        card.getChildren().addAll(
                lockIcon, lblTitle, lblCompliance,
                officerInfo, form, btnUnlock, btnSignOut
        );

        this.getChildren().add(card);

        // Fade in
        this.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), this);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();

        // Focus PIN field
        txtPin.requestFocus();
    }

    private void handleUnlock() {
        String pin = txtPin.getText();
        boolean unlocked = SessionAutoLockManager.getInstance().unlockSession(
                pin, navManager.getOfficerName(), navManager.getAgencyId(), navManager.getRole()
        );

        if (unlocked) {
            lblError.setVisible(false);
            // Close overlay
            if (this.getParent() instanceof Pane parentPane) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), this);
                ft.setFromValue(1); ft.setToValue(0);
                ft.setOnFinished(ev -> parentPane.getChildren().remove(this));
                ft.play();
            }
        } else {
            lblError.setText("⚠️ Invalid PIN! Authentication failed.");
            lblError.setVisible(true);
            txtPin.selectAll();
            txtPin.requestFocus();
        }
    }
}
