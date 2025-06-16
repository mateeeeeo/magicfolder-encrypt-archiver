package com.magicfolder;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.File;
import java.text.DateFormat;
import java.util.*;

public class FileNode extends DictFile {
    private StringProperty fileName;
    private StringProperty fileSizeStr;
    private StringProperty lastModifified;

    private boolean isNew = false;

    public StringProperty nameProperty() {
        if (fileName == null) fileName = new SimpleStringProperty(this, "name");
        return fileName;
    }

    private static final Map<String, Long> SIZE_UNITS = new LinkedHashMap<>() {{
        put("PB", 1024L * 1024 * 1024 * 1024 * 1024);
        put("TB", 1024L * 1024 * 1024 * 1024);
        put("GB", 1024L * 1024 * 1024);
        put("MB", 1024L * 1024);
        put("KB", 1024L);
        put("B", 1L);
    }};

    public StringProperty sizeProperty() {
        if (fileSizeStr == null) fileSizeStr = new SimpleStringProperty(this, "size");
        return fileSizeStr;
    }

    public StringProperty lastModifiedProperty() {
        if (lastModifified == null) lastModifified = new SimpleStringProperty(this, "lastModified");
        return lastModifified;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    private void init(String fileName, long fileSize, long lastModified, byte[] iv) {
        this.nameProperty().set(fileName);

        String formattedSize = "-1";

        for (var entry : SIZE_UNITS.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();

            if (value < fileSize) {
                System.out.println(value);
                String temp = Long.toString(fileSize / value);
                formattedSize = temp.substring(0, Math.min(3, temp.length())) + key;
                break;
            }
        }

        if (super.isDir) {
            this.sizeProperty().set("");
            this.lastModifiedProperty().set("");
        } else {
            this.sizeProperty().set(formattedSize);
            DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT,
                    new Locale.Builder().setLanguage("en").setRegion("US").build());

            this.lastModifiedProperty().set(dateFormat.format(new Date(lastModified)));
        }
    }

    public FileNode(String fileName, String fileId, long fileSize, long lastModified, byte[] iv, boolean isDir) {
        super(fileName, fileId, fileSize, lastModified, iv, isDir);
        init(fileName, fileSize, lastModified, iv);
    }

    public FileNode(File file) {
        super(file);

        this.isNew = true;
        this.isDir = file.isDirectory();
        init(file.getName(), file.length(), file.lastModified(), null);
    }

    public FileNode(File file, boolean isDir) {
        super(file);

        super.isDir = isDir;
        init(file.getName(), file.length(), file.lastModified(), null);
    }

    public FileNode(DictFile file) {
        super(file.getName(), file.getId(), file.getSize(), file.getLastModified(), file.getIv(), file.isDir());
        init(file.getName(), file.getSize(), file.getLastModified(), file.getIv());
    }

    public FileNode() {
        super();
        this.nameProperty().set("Untitled Folder");
        this.lastModifiedProperty().set("");
        this.sizeProperty().set("");
        this.isNew = true;
        this.isDir = true;
    }
}
