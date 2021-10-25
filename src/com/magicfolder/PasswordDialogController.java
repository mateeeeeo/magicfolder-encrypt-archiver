package com.magicfolder;

import com.magicfolder.helpers.BCrypt;
import com.magicfolder.helpers.Folder;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.security.spec.KeySpec;
import java.util.Arrays;

public class PasswordDialogController {
    @FXML
    private Button submitBtn;
    @FXML
    private Label incorrectPwLabel;
    @FXML
    private TextField pwField;

    private byte[] bcryptHash;
    private File folder;

    public void setBcryptHash(byte[] bcryptHash) {
        this.bcryptHash = bcryptHash;
    }

    public void setFolder(File folder) {
        this.folder = folder;
    }

    public void onSubmit() {
        try {
            FXMLLoader openFolderLoader = new FXMLLoader(getClass().getResource("fxml/OpenFolderPage.fxml"));
            HBox openFilesContainer = openFolderLoader.load();
            OpenFolderPageController openFolderPageController = openFolderLoader.getController();

            String passwordPlain = pwField.getText();
            byte[] salt = Arrays.copyOfRange(bcryptHash, 1, 30);
            byte[] passwordHash = Arrays.copyOfRange(bcryptHash, 1, 61);
            byte[] iv = Arrays.copyOfRange(bcryptHash, 61, bcryptHash.length);

            if (BCrypt.checkpw(passwordPlain, new String(passwordHash))) {
                KeySpec keySpec = new PBEKeySpec(passwordPlain.toCharArray(), salt, 10, 128);
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

                Folder magicFolder = new Folder(folder.getName(), folder.getPath());

                byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();
                openFolderPageController.setFolder(magicFolder, key, iv);

                Stage openFolderStage = (Stage) submitBtn.getScene().getWindow();
                openFolderStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent windowEvent) {
                        System.out.println("Deleted temp files");
                        openFolderPageController.deleteTempFiles();
                    }
                });
                openFolderStage.setScene(new Scene(openFilesContainer, 840, 600));
            } else {
                incorrectPwLabel.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            onSubmit();
        }
    }

//    public void onSubmit() {
//        VBox openFilesContainer = new VBox();
//
//        DecimalFormat decimalFormat = new DecimalFormat("#,###");
//
//        String passwordPlain = pwField.getText();
//        byte[] salt = Arrays.copyOfRange(bcryptHash, 1, 30);
//        byte[] passwordHash = Arrays.copyOfRange(bcryptHash, 1, 61);
//        byte[] iv = Arrays.copyOfRange(bcryptHash, 61, bcryptHash.length);
//
//        try {
//            if (BCrypt.checkpw(passwordPlain, new String(passwordHash))) {
//                KeySpec keySpec = new PBEKeySpec(passwordPlain.toCharArray(), salt, 10, 128);
//                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
//                Folder magicFolder = new Folder(folder.getName(), folder.getPath());
//                ArrayList<String> fileNames = magicFolder.getFileNames(secretKeyFactory.generateSecret(keySpec).getEncoded(), iv);
//                ArrayList<Long> fileSizes = magicFolder.getFileSizes(secretKeyFactory.generateSecret(keySpec).getEncoded(), iv);
//
//                for (int i = 0; i < fileNames.size(); i++) {
//                    Label fileName = new Label((i + 1) + ". " + fileNames.get(i));
//                    fileName.setTextFill(Color.WHITE);
//                    fileName.getStyleClass().add("file-text");
//                    Label fileSize = new Label("" + decimalFormat.format(fileSizes.get(i)));
//                    fileSize.setTextFill(Color.WHITE);
//                    fileSize.getStyleClass().add("file-text");
//                    HBox space = new HBox();
//                    HBox.setHgrow(space, Priority.ALWAYS);
//                    HBox.setHgrow(openFilesContainer, Priority.ALWAYS);
//                    HBox fileContainer = new HBox();
//                    fileContainer.setSpacing(10);
//                    fileContainer.getChildren().addAll(new CheckBox(), fileName, space, fileSize);
//                    openFilesContainer.getChildren().add(fileContainer);
//                }
//
//                openFilesContainer.setPadding(new Insets(20));
//                openFilesContainer.setSpacing(5);
//
//                VBox openFolderRoot = FXMLLoader.load(getClass().getResource("OpenFolderPageMenuBar.fxml"));
//                ((HBox) openFolderRoot.getChildren().get(1)).getChildren().add(openFilesContainer);
//                ((HBox) openFolderRoot.getChildren().get(1)).getChildren().get(0).setOnMouseClicked((event1 -> {
//                    try {
//                        for (Node fileContainer1 : openFilesContainer.getChildren()) {
//                            if (((CheckBox) ((HBox) fileContainer1).getChildren().get(0)).isSelected()) {
//                                new Folder(folder.getName(), folder.getPath()).extract(((Label) ((HBox) fileContainer1).getChildren().get(1)).getText().substring(3),
//                                        secretKeyFactory.generateSecret(keySpec).getEncoded(), iv);
//                            }
//                        }
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }));
//
//                Stage openFolderStage = (Stage) submitBtn.getScene().getWindow();
//                openFolderStage.setScene(new Scene(openFolderRoot, 840, 600));
//            } else {
//                incorrectPwLabel.setVisible(true);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
