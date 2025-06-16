package com.magicfolder.components;

import javafx.scene.layout.HBox;

import java.util.Properties;


public class FileTreeTableViewToolbar extends HBox {

    public enum TOOLBAR_ACTION {
        DELETE, RENAME, ADD_FILE, ADD_FOLDER, EXTRACT, SAVE, LOCK
    }

    public static interface ToolbarIconClickedCallback {
        void run(TOOLBAR_ACTION action);
    }

    private ToolbarIconClickedCallback toolbarIconClickedHandler;
    private FileTreeTableView.Mode mode = FileTreeTableView.Mode.CREATE_ARCHIVE;

    public FileTreeTableView.Mode getMode() {
        return mode;
    }

    public void setMode(FileTreeTableView.Mode mode) {
        this.mode = mode;
        init();
    }

    public void setOnToolbarIconClicked(ToolbarIconClickedCallback callback) {
        this.toolbarIconClickedHandler = callback;
    }


    public FileTreeTableViewToolbar() {
        init();
    }

    private HBox createSeparator() {
        HBox separator = new HBox();
        separator.setPrefWidth(1);
        separator.setStyle("-fx-border-width:0 0 0 1px; -fx-border-style: solid; -fx-border-color: #364149;");
        return separator;
    }

    private void init() {
        getChildren().clear();
        Properties props = System.getProperties();
        props.setProperty("swing.jlf.contentPaneTransparent", "true");

        setSpacing(16);

        System.out.println(getClass().getResource("/com/magicfolder/resources/icons/add_file.svg").toExternalForm());
        FXSVGIcon addIcon = new FXSVGIcon(getClass().getResource("/com/magicfolder/resources/icons/add_file.svg"), 24, 24);
        addIcon.setOnMouseClicked(event -> {
            this.toolbarIconClickedHandler.run(TOOLBAR_ACTION.ADD_FILE);
        });
        getChildren().add(addIcon);

        FXSVGIcon folderAddIcon = new FXSVGIcon(getClass().getResource("/com/magicfolder/resources/icons/new_folder.svg"), 24, 24);
        folderAddIcon.setOnMouseClicked(event -> {
            this.toolbarIconClickedHandler.run(TOOLBAR_ACTION.ADD_FOLDER);
        });
        getChildren().add(folderAddIcon);

        FXSVGIcon renameIcon = new FXSVGIcon(getClass().getResource("/com/magicfolder/resources/icons/edit.svg"), 24, 24);
        renameIcon.setOnMouseClicked(event -> {
            this.toolbarIconClickedHandler.run(TOOLBAR_ACTION.RENAME);
        });
        getChildren().add(renameIcon);

        FXSVGIcon trashIcon = new FXSVGIcon(getClass().getResource("/com/magicfolder/resources/icons/trash.svg"), 24, 24);
        trashIcon.setOnMouseClicked(event -> {
            this.toolbarIconClickedHandler.run(TOOLBAR_ACTION.DELETE);
        });
        getChildren().add(trashIcon);

        var separator1 = createSeparator();
        getChildren().add(separator1);

        if (mode == FileTreeTableView.Mode.EDIT_ARCHIVE) {


            FXSVGIcon extractIcon = new FXSVGIcon(this.getClass().getResource("/com/magicfolder/resources/icons/unarchive.svg"), 24, 24);
            extractIcon.setOnMouseClicked(event -> {
                this.toolbarIconClickedHandler.run(TOOLBAR_ACTION.EXTRACT);
            });
            getChildren().add(extractIcon);

            var separator2 = createSeparator();
            getChildren().add(separator2);
            FXSVGIcon saveIcon = new FXSVGIcon(getClass().getResource("/com/magicfolder/resources/icons/save.svg"), 24, 24);
            saveIcon.setOnMouseClicked(event -> {
                this.toolbarIconClickedHandler.run(TOOLBAR_ACTION.SAVE);
            });
            getChildren().add(saveIcon);
        } else if(mode == FileTreeTableView.Mode.CREATE_ARCHIVE) {
            FXSVGIcon lockIcon = new FXSVGIcon(getClass().getResource("/com/magicfolder/resources/icons/lock.svg"), 24, 24);
            lockIcon.setOnMouseClicked(event -> {
                this.toolbarIconClickedHandler.run(TOOLBAR_ACTION.LOCK);
            });
            getChildren().add(lockIcon);
        }

        requestLayout();
    }
}
