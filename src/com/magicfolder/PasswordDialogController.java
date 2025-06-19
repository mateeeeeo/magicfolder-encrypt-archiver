package com.magicfolder;

import com.magicfolder.helpers.BCrypt;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import com.magicfolder.helpers.SecureArchive;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
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
            FXMLLoader openFolderLoader = new FXMLLoader(getClass().getResource("/fxml/OpenFolderPage.fxml"));
            StackPane openFilesContainer = openFolderLoader.load();
            OpenArchivePageController openArchivePageController = openFolderLoader.getController();

            String passwordPlain = pwField.getText();
            byte[] salt = Arrays.copyOfRange(bcryptHash, 1, 30);
            byte[] passwordHash = Arrays.copyOfRange(bcryptHash, 1, 61);
            byte[] iv = Arrays.copyOfRange(bcryptHash, 61, bcryptHash.length);

            if (BCrypt.checkpw(passwordPlain, new String(passwordHash))) {
                KeySpec keySpec = new PBEKeySpec(passwordPlain.toCharArray(), salt, 10, 128);
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                byte[] key = secretKeyFactory.generateSecret(keySpec).getEncoded();

                SecureArchive magicFolder = new SecureArchive(folder.getName(), folder.getPath(), passwordPlain, salt);
                magicFolder.getDecryptKeyIv2PairAndIvDict(passwordPlain);
                magicFolder.getKeyIv1Pair();
                magicFolder.readDict();
                openArchivePageController.setFolder(magicFolder, passwordPlain, salt);

                Stage openFolderStage = (Stage) submitBtn.getScene().getWindow();
                openFolderStage.setOnCloseRequest(windowEvent -> {
                    System.out.println("Deleted temp files");
                });
                submitBtn.getScene().getWindow().setWidth(840);
                submitBtn.getScene().getWindow().setHeight(600);
                submitBtn.getScene().getWindow().centerOnScreen();
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
}
