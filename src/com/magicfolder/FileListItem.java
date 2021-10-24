package com.magicfolder;

import com.magicfolder.helpers.Folder;

public class FileListItem {
    private int fileId;
    private String fileName;
    private long fileSize;
    private Folder folder;
    private byte[] folderKey;
    private byte[] folderIv;

    public byte[] getFolderKey() {
        return folderKey;
    }

    public byte[] getFolderIv() {
        return folderIv;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public Folder getFolder() {
        return folder;
    }

    public FileListItem(int fileId, String fileName, long fileSize, Folder folder, byte[] folderKey, byte[] folderIv) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.folder = folder;
        this.folderKey = folderKey;
        this.folderIv = folderIv;
    }
}
