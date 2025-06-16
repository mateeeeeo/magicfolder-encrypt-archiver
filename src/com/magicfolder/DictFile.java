package com.magicfolder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class DictFile {
    private File file;
    private String name;
    private String path = "";
    private long size;
    private long lastModified;
    private byte[] iv = new byte[16];
    private String id;

    protected boolean isDir;

    private List<DictFile> children = new ArrayList<>();

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
        this.path = file.getPath();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public byte[] getIv() {
        return iv;
    }

    public void setIv(byte[] iv) {
        this.iv = iv;
    }

    public String getId() {
        return id;
    }


    public boolean isDir() {
        return isDir;
    }

    public List<DictFile> getChildren() {
        return children;
    }

    public JSONObject toJSONObject() {
        JSONObject res = new JSONObject();
        res.put("name", this.name);
        res.put("size", this.size);
        res.put("last_modified", this.lastModified);
        res.put("iv", Base64.getEncoder().encodeToString(this.iv));
        res.put("children", new JSONArray());
        res.put("id", this.id);
        res.put("is_dir", this.isDir);
        return res;
    }

    public DictFile() {
        this.name = "Untitled Folder";
        this.lastModified = -1;
        this.size = -1;
        this.id = UUID.randomUUID().toString();
        initIv();
    }

    public DictFile(String name, String id, long size, long lastModified, byte[] iv, boolean isDir) {
        this.name = name;
        this.size = size;
        this.lastModified = lastModified;
        this.iv = iv;
        this.id = id;
        this.isDir = isDir;
    }

    public DictFile(File file) {
        this.file = file;
        this.name = file.getName();
        this.path = file.getPath();
        this.size = file.length();
        this.lastModified = file.lastModified();
        this.id = UUID.randomUUID().toString();
        initIv();
    }

    private void initIv() {
        new SecureRandom().nextBytes(this.iv);
    }
}
