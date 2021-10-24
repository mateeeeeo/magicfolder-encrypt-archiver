package com.magicfolder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

public class Main extends Application {

    private static String openedFilePath = null;

    @Override
    public void start(Stage primaryStage) {
        Font.loadFont(getClass().getResource("/fonts/ModernSans-Light.otf").toExternalForm(), 16);
        try {
            if(openedFilePath == null) {
                Parent root = FXMLLoader.load(getClass().getResource("fxml/FoldersPage.fxml"));
                primaryStage.setTitle("MagicFolder - Terminal");
                primaryStage.setScene(new Scene(root, 800, 600));
                primaryStage.show();
            } else {
                Stage openFolderWindow = new Stage();
                openFolderWindow.setTitle("MagicFolder - Open");
                FXMLLoader passwordPageLoader = new FXMLLoader(getClass().getResource("fxml/PasswordPage.fxml"));
                VBox openFolderRoot = passwordPageLoader.load();
                PasswordDialogController passwordDialogController = passwordPageLoader.getController();

                byte[] buffer = new byte["!".length() + 60 + 16];
                FileInputStream fis = new FileInputStream(openedFilePath);
                fis.read(buffer);

                if (new String(buffer).contains("!")) {
                    openFolderWindow.setScene(new Scene(openFolderRoot, 840, 600));
                    passwordDialogController.setBcryptHash(buffer);
                    passwordDialogController.setFolder(new File(openedFilePath));
                }
                openFolderWindow.show();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println(Arrays.toString(args));

        if(args.length > 0) {
            openedFilePath = args[0];
        }

        launch(args);
    }
}
