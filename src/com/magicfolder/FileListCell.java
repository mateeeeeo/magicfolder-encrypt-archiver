package com.magicfolder;

import com.magicfolder.helpers.Folder;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.ResourceBundle;

public class FileListCell extends ListCell<FileListItem> {
    @FXML
    private Label idLabel;
    @FXML
    private Label filenameLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    private VBox cellContainer;

    private final String tempDirPath = System.getProperty("java.io.tmpdir");

    private DecimalFormat decimalFormat = new DecimalFormat("#,###");

    private HBox container = null;
    private Image fxIconImg = null;
    private FXMLLoader loader = null;

    private void extractAndRunFile(FileListItem fileListItem) {
        try {
            File file = new File(tempDirPath + "/" + fileListItem.getFileName());
            fileListItem.getFolder().extract(fileListItem.getFileName(), tempDirPath,
                    fileListItem.getFolderKey(), fileListItem.getFolderIv());

            Desktop.getDesktop().open(file);
            file.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateItem(FileListItem item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty && item != null) {
            if (loader == null) {
                loader = new FXMLLoader(getClass().getResource("fxml/FileListItem.fxml"));
                try {
                    container = loader.load();
                } catch (Exception e) {
                    e.printStackTrace();
                    setGraphic(null);
                    return;
                }
            }

            File extractedFile = new File(tempDirPath + "/" + item.getFileName());
            if (!extractedFile.exists()) {
                item.getFolder().extract(item.getFileName(), tempDirPath, item.getFolderKey(), item.getFolderIv());
            }

            if(fxIconImg == null) {
                BufferedImage icon = (BufferedImage) ((ImageIcon) FileSystemView.getFileSystemView()
                        .getSystemIcon(extractedFile)).getImage();

                fxIconImg = SwingFXUtils.toFXImage(icon, null);
            }

            ((ImageView) container.getChildren().get(0)).setImage(fxIconImg);
            // Set file name label text
            ((Label) container.getChildren().get(1)).setText(item.getFileName());
            // Set file size label text
            ((Label) container.getChildren().get(3)).setText(decimalFormat.format(item.getFileSize()));
            // Add drag & drop
            container.setOnDragDetected(event -> {
                Dragboard dragboard = container.startDragAndDrop(TransferMode.MOVE);

                ClipboardContent content = new ClipboardContent();
                content.putFiles(Collections.singletonList(extractedFile));

                dragboard.setDragView(fxIconImg);
                dragboard.setContent(content);
            });

            container.setOnMouseClicked(event -> {
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    if (event.getClickCount() == 2) {
                        extractAndRunFile(item);
                    }
                }
            });

            setGraphic(container);
        } else {
            setGraphic(null);
        }
    }
}
