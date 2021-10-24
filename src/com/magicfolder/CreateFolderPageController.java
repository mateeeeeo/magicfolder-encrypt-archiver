package com.magicfolder;

import com.magicfolder.helpers.Folder;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class CreateFolderPageController implements Initializable {
    @FXML
    private TextField createFolderNameField;
    @FXML
    private PasswordField createFolderPasswordField;
    @FXML
    private TextField createFolderPathField;
    @FXML
    private Button browseFilesBtn;
    @FXML
    private VBox filesContainer;

    private List<File> files;

    public void setFiles(List<File> files) {
        this.files = files;
    }

    public void displayFiles() {
        if (!files.isEmpty()) {
            String fileNameNoExtension = files.get(0).getName().substring(0, files.get(0).getName().lastIndexOf('.'));
            createFolderNameField.setText(fileNameNoExtension);

            String filePath = files.get(0).getPath().replace(files.get(0).getName(), "");
            createFolderPathField.setText(filePath);

            Label fileSize;
            Label fileName;
            HBox fileContainer;
            HBox space;
            ImageView iconImageView;
            BufferedImage icon;
                    DecimalFormat decimalFormat = new DecimalFormat("#,###");

            int i = 1;
            for (File file : files) {
                icon = (BufferedImage) ((ImageIcon) FileSystemView.getFileSystemView()
                        .getSystemIcon(file)).getImage();

                iconImageView = new ImageView(SwingFXUtils.toFXImage(icon, null));
                iconImageView.setFitWidth(16);
                iconImageView.setFitHeight(16);

                fileSize = new Label(decimalFormat.format(file.length()));
                fileSize.setTextFill(Color.WHITE);

                fileName = new Label(i + ". " + file.getName());
                fileName.setTextFill(Color.WHITE);

                space = new HBox();
                HBox.setHgrow(space, Priority.ALWAYS);

                fileContainer = new HBox();
                fileContainer.getChildren().addAll(iconImageView, fileName, space, fileSize);
                HBox.setHgrow(fileContainer, Priority.ALWAYS);

                filesContainer.getChildren().add(fileContainer);
                i++;
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        files = new ArrayList<>();

//        createFolderPathField.textProperty().addListener(((observableValue, oldValue, newValue) -> {
//            onPathChanged(newValue);
//        }));

//        createFolderNameField.textProperty().addListener(((observableValue, oldValue, newValue) -> {
//            onNameChanged(oldValue, newValue);
//        }));
    }

    @FXML
    public void onCreateFolderButtonClicked() {
        if(files.size() > 0) {
            Folder folder = new Folder(createFolderNameField.getText(),
                    createFolderPathField.getText() + createFolderNameField.getText() + ".mgf",
                    createFolderPasswordField.getText(), files);
            folder.createArchive();
        }
    }

    public void onNameChanged(String oldVal, String newVal) {
        String currentPath = createFolderPathField.getText();

        String newPath = currentPath.replaceFirst("(?s)"+oldVal+"(?!.*?"+oldVal+")", newVal);
        System.out.println(newPath);
        createFolderPathField.setText(newPath);
//        int fileNameIndex = currentPath.lastIndexOf(oldVal) + 1;
//        StringBuilder sb = new StringBuilder(currentPath);
//        sb.delete(fileNameIndex, currentPath.length() - 4);
//        sb.insert(fileNameIndex, newVal);
//        System.out.println(sb.toString());
//        createFolderPathField.setText(sb.toString());
    }

//    public void onPathChanged(String newVal) {
//        int fileNameStartIndex = newVal.lastIndexOf("\\") + 1;
//        String fileName = newVal.substring(fileNameStartIndex).replace(".mgf", "");
//
//        createFolderNameField.setText(fileName);
//    }

    @FXML
    public void openFileChooser() {
        Window window = browseFilesBtn.getScene().getWindow();

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName(createFolderNameField.getText());
        fileChooser.setInitialDirectory(new File(createFolderPathField.getText().replace(createFolderNameField.getText() + ".mgf", "")));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Magic Folder(*.mgf)", "*.mgf"));
        File file = fileChooser.showSaveDialog(window);

        if(file != null) {
            String fileName = file.getName();
            String fileNameWithoutExtension = fileName.substring(0, fileName.length() - 4);
            createFolderNameField.setText(fileNameWithoutExtension);
            createFolderPathField.setText(file.getPath().replace(fileName, ""));
        }
    }
}
