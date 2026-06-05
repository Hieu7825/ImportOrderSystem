package com.importorder.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class NavigationHelper {

    public static void navigateTo(String fxmlPath, Stage currentStage) {
        try {
            FXMLLoader loader = new FXMLLoader(
                NavigationHelper.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load(), 1280, 720);
            scene.getStylesheets().add(
                NavigationHelper.class.getResource("/css/global.css").toExternalForm());
            currentStage.setScene(scene);
        } catch (Exception e) {
            AlertUtils.showError("Lỗi điều hướng", e.getMessage());
        }
    }

    public static void openChangePassword(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                NavigationHelper.class.getResource("/fxml/ChangePassword.fxml"));
            Scene scene = new Scene(loader.load(), 500, 400);
            scene.getStylesheets().add(
                NavigationHelper.class.getResource("/css/global.css").toExternalForm());
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(owner);
            dialog.setTitle("Đổi mật khẩu");
            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            AlertUtils.showError("Lỗi", e.getMessage());
        }
    }
}