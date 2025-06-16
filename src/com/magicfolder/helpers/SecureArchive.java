package com.magicfolder.helpers;

import com.magicfolder.DictFile;
import com.magicfolder.FileNode;

import javafx.scene.control.TreeItem;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;


public class SecureArchive {

    private String name;
    private String path;
    private String password;
    private TreeItem<FileNode> dictRoot;
    private String jsonDictStr;
    private List<File> files;
    public static final int GCM_AUTHENTICATION_TAG_LEN = 16 * 8; // 16 bytes to bits
    public static final int KEY_AND_IV_LENGTH = 16;
    public static final int BCRYPT_HASH_LENGTH = 60;
    public static final int PASSWORD_DECORATOR_LENGTH = 1;
    private SecretKey decryptKey1;

    private byte[] decryptIv1;

    private SecretKey decryptKey2;

    private byte[] decryptIv2;
    private byte[] decryptIvDict;
    private byte[] bcryptSalt;
    private long dictLen = 0;

    private TreeItem<FileNode> originalDict = null;

    public SecureArchive(String name, String path, String password, TreeItem<FileNode> dictRoot, List<File> files) {
        this.name = name;
        this.path = path;
        this.password = password;
        this.dictRoot = dictRoot;
        this.files = files;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public TreeItem<FileNode> getDictRoot() {
        return dictRoot;
    }

    public TreeItem<FileNode> getOriginalDict() {
        return originalDict;
    }

    public void setOriginalDict(TreeItem<FileNode> originalDict) {
        this.originalDict = originalDict;
    }

    public void setDictRoot(TreeItem<FileNode> dictRoot) {
        this.dictRoot = dictRoot;
    }

    // For decryption
    public SecureArchive(String name, String path, String password, byte[] bcryptSalt) {
        this.name = name;
        this.path = path;
        this.password = password;
        this.bcryptSalt = bcryptSalt;
    }

    private void createJSONDict(TreeItem<FileNode> dictNode, JSONObject jsonNode) {
        // converts JavaFX's TreeTableView (tree structure) into its equivalent JSON tree
        for (int i = 0; i < dictNode.getChildren().size(); i++) {
            var treeItem = dictNode.getChildren().get(i);
            var treeItemValue = treeItem.getValue(); // get the FileNode instance
            var child = treeItemValue.toJSONObject();
            createJSONDict(treeItem, child); // recursively add children to tree
            ((JSONArray) jsonNode.get("children")).put(child);
        }
    }

    private void traverseDictAndWriteFiles(TreeItem<FileNode> node, FileOutputStream fos, SecretKey key, Cipher cipher) throws Exception {
        // recursively traverses file tree, encrypts file data
        byte[] buffer = new byte[8192];

        for (TreeItem<FileNode> c : node.getChildren()) {
            var childValue = c.getValue();
            if (!childValue.isDir()) {
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LEN, childValue.getIv()));
                FileInputStream fis = new FileInputStream(childValue.getPath());
                int len;

                while ((len = fis.read(buffer)) != -1) {
                    // read all file contents
                    byte[] output = cipher.update(buffer, 0, len);
                    if (output != null)
                        fos.write(output);
                }
                byte[] finalBlock = cipher.doFinal(); // apply AES-GCM tag
                if (finalBlock != null) fos.write(finalBlock); // write tag to encrypted output
                fis.close();
            }

            traverseDictAndWriteFiles(c, fos, key, cipher);
        }
    }

    public void commitArchive() {
        // save the archive with the same cryptographic keys, IVs and salt (used when saving the archive)
        createArchive(this.decryptKey1, this.decryptIv1, this.decryptKey2, this.decryptIv2, this.decryptIvDict, this.bcryptSalt);
    }

    public void createArchiveWithRandomKeys() {
        // new archive
        try {
            byte[] iv1 = new byte[16];
            byte[] iv2 = new byte[16];
            byte[] ivDict = new byte[16];

            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1"); // encrypts the user set password
            SecureRandom srandom = new SecureRandom();

            byte[] salt = BCrypt.gensalt().getBytes();
            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 10, 128); // used to generate a strong encryption key from the user's password

            SecretKey skey1 = keygen.generateKey();
            SecretKey key2 = secretKeyFactory.generateSecret(keySpec);
            SecretKey skey2 = new SecretKeySpec(key2.getEncoded(), "AES");

            srandom.nextBytes(iv1);
            srandom.nextBytes(iv2);
            srandom.nextBytes(ivDict);

            createArchive(skey1, iv1, skey2, iv2, ivDict, salt);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createArchive(SecretKey key1, byte[] iv1, SecretKey key2, byte[] iv2, byte[] ivDict, byte[] bcryptSalt) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            IvParameterSpec ivspec;

            Cipher cipher;

            // the '!'-decorator indicates to the program that the folder is password protected
            if (!this.password.isEmpty()) {
                fos.write("!".getBytes());
            }

            // write the bcrypt hash to the password
            String bcryptPasswordHash = BCrypt.hashpw(this.password, new String(bcryptSalt, StandardCharsets.UTF_8));
            byte[] bcryptPasswordHashBytes = bcryptPasswordHash.getBytes();
            fos.write(bcryptPasswordHashBytes);

            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LEN, iv2);
            cipher.init(Cipher.ENCRYPT_MODE, key2, gcmSpec);

            fos.write(iv2);
            fos.write(ivDict);

            byte[] encodedSkey1 = key1.getEncoded();
            baos.write(encodedSkey1);
            baos.write(iv1);
            baos.close();

            byte[] encryptedKey1AndIv1 = cipher.doFinal(baos.toByteArray());
            fos.write(encryptedKey1AndIv1);

            JSONObject jsonDictRoot = new JSONObject();
            jsonDictRoot.put("children", new JSONArray());
            createJSONDict(this.dictRoot, jsonDictRoot);

            String jsonDictStr = jsonDictRoot.toString();
            System.out.println(jsonDictStr);

            // encodes the json dict length from an int to a 32 bit integer (4 exact characters)
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream dataOut = new DataOutputStream(byteOut);
            dataOut.writeInt(jsonDictStr.length());
            dataOut.flush();
            System.out.println(Arrays.toString(byteOut.toByteArray()));
            fos.write(byteOut.toByteArray());
            dataOut.close();

            gcmSpec = new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LEN, ivDict);
            cipher.init(Cipher.ENCRYPT_MODE, key1, gcmSpec);

            byte[] encryptedDict = cipher.doFinal(jsonDictStr.getBytes());
            fos.write(encryptedDict);

            traverseDictAndWriteFiles(this.dictRoot, fos, key1, cipher);

            this.decryptIv1 = iv1;
            this.decryptIv2 = iv2;
            this.decryptIvDict = ivDict;
            this.decryptKey1 = key1;
            this.decryptKey2 = key2;
            this.bcryptSalt = bcryptSalt;

            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getDecryptKeyIv2PairAndIvDict(String passwordPlain) throws Exception {
        FileInputStream fis = null;
        try {
            byte[] buffer = new byte["!".length() + 60 + 16 + 16]; // contains !-decorator (if encrypted), bcrypt hash, iv2
            fis = new FileInputStream(this.path);
            fis.read(buffer);


            byte[] salt = Arrays.copyOfRange(buffer, 1, 30);
            byte[] passwordHash = Arrays.copyOfRange(buffer, 1, 61);

            if (!BCrypt.checkpw(passwordPlain, new String(passwordHash))) {
                throw new Exception("Wrong Password!");
            }

            KeySpec keySpec = new PBEKeySpec(passwordPlain.toCharArray(), salt, 10, 128);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            this.decryptKey2 = secretKeyFactory.generateSecret(keySpec);
            this.decryptKey2 = new SecretKeySpec(this.decryptKey2.getEncoded(), "AES");
            this.decryptIv2 = Arrays.copyOfRange(buffer, 61, 77);
            this.decryptIvDict = Arrays.copyOfRange(buffer, 77, 93);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fis != null)
                    fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addIvToEachFile(JSONObject root) {
        JSONArray children = (JSONArray) root.get("children");
        for (int i = 0; i < children.length(); i++) {
            var child = children.getJSONObject(i);
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            child.put("iv", Base64.getEncoder().encodeToString(iv));
            addIvToEachFile(child);
        }
    }

    public String readDict() {
        String dict = "";

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec skey1 = new SecretKeySpec(this.decryptKey1.getEncoded(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LEN, this.decryptIvDict);

            FileInputStream fis = new FileInputStream(this.path);
            // RECREATES DICTIONARY
            fis.skip(141);
            DataInputStream dataIn = new DataInputStream(fis);
            int dictLen = dataIn.readInt();
            this.dictLen = dictLen;

            cipher.init(Cipher.DECRYPT_MODE, skey1, gcmSpec);

            byte[] dictBuffer = new byte[dictLen + 16];
            fis.read(dictBuffer);
            dict = new String(cipher.doFinal(dictBuffer));
            this.jsonDictStr = dict;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dict;
    }

    public void parseDict(TreeItem<FileNode> treeNode, JSONObject jsonNode) {
        for (Object child : jsonNode.getJSONArray("children")) {
            var jsonChild = (JSONObject) child;
            var treeChildValue = new FileNode(jsonChild.getString("name"), jsonChild.getString("id"), jsonChild.getLong("size"),
                    jsonChild.getLong("last_modified"), Base64.getDecoder().decode(jsonChild.getString("iv")),
                    jsonChild.getBoolean("is_dir"));

            var treeChild = new TreeItem<>(treeChildValue);
            treeNode.getChildren().add(treeChild);
            parseDict(treeChild, jsonChild);
        }
    }

    public void getKeyIv1Pair() {
        try (FileInputStream fis = new FileInputStream(this.path)) {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec skey2 = new SecretKeySpec(this.decryptKey2.getEncoded(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LEN, this.decryptIv2);

            //  IvParameterSpec ivSpec = new IvParameterSpec(iv2);

            final int KEY_IV_1_PAIR_ENCRYPTED_LENGTH = 48;
            byte[] buffer = new byte[KEY_IV_1_PAIR_ENCRYPTED_LENGTH];
            byte[] keyIv1Pair;

            fis.skip(93);
            fis.read(buffer);

            cipher.init(Cipher.DECRYPT_MODE, skey2, gcmSpec);

            keyIv1Pair = cipher.doFinal(buffer);
            var encodedKey1 = Arrays.copyOfRange(keyIv1Pair, 0, 16);
            this.decryptKey1 = new SecretKeySpec(encodedKey1, 0, encodedKey1.length, "AES");
            this.decryptIv1 = Arrays.copyOfRange(keyIv1Pair, 16, 32);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void flattenDict(JSONObject node, List<DictFile> flattened) {
        JSONArray children = (JSONArray) node.get("children");
        for (int i = 0; i < children.length(); i++) {
            var child = children.getJSONObject(i);
            if (((JSONArray) child.get("children")).isEmpty()) {
                var test = Base64.getDecoder().decode(child.getString("iv"));
                flattened.add(new DictFile(child.getString("name"), child.getString("id"), child.getLong("size"),
                        child.getLong("last_modified"), Base64.getDecoder().decode(child.getString("iv")), child.getBoolean("is_dir")));
            }
            flattenDict(child, flattened);
        }
    }

    public void extract(String fileId, String extractPath) {
        try {
            FileInputStream fis;
            System.out.println(extractPath);

            FileOutputStream fos = new FileOutputStream(extractPath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();


            var skey1 = new SecretKeySpec(this.decryptKey1.getEncoded(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LEN, this.decryptIv1);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skey1, gcmSpec);

            fis = new FileInputStream(this.path);

            JSONObject dictRoot = new JSONObject(this.jsonDictStr);
            List<DictFile> flattenedFilesList = new ArrayList<>();
            flattenDict(dictRoot, flattenedFilesList);

            // for each file up until desired file:
            //   skip file length in bytes
            // read contents of file up to file size
            //GETS SIZE OF FILE TO BE EXTRACTED
            byte[] buffer = new byte[32 * 1024];

            final long HEADER_AND_DICT_LEN = 161;
            fis.skip(this.dictLen + HEADER_AND_DICT_LEN);

            int tagLength = 16;
            long totalRead = 0;
            int bytesRead;

            byte[] fileIv = null;
            long fileSize = -1;

            for (DictFile file : flattenedFilesList) {
                if (file.getId().equals(fileId)) {
                    fileIv = file.getIv();
                    fileSize = file.getSize();
                    break;
                }
                fis.skip(file.getSize() + 16);
            }

            if (fileIv == null) {
                return;
            }

            gcmSpec = new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LEN, fileIv);
            cipher.init(Cipher.DECRYPT_MODE, skey1, gcmSpec);

            if(fileSize > buffer.length) {
                while (((bytesRead = fis.read(buffer)) != -1)) {
                    totalRead += bytesRead;
                    byte[] plain = cipher.update(buffer, 0, bytesRead);
                    if (plain != null) fos.write(plain);

                    if (totalRead + buffer.length > fileSize) {
                        long n = fileSize - totalRead + 16;
                        bytesRead = fis.readNBytes(buffer, 0, (int) n);
                        plain = cipher.update(buffer, 0, bytesRead);
                        if (plain != null) fos.write(plain);
                        break;
                    }
                }
                byte[] finalBlock = cipher.doFinal();
                if (finalBlock != null) fos.write(finalBlock);
            } else {
                fos.write(cipher.doFinal(fis.readNBytes((int) (fileSize) + 16)));
            }

            fis.close();
            fos.close();
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void extractAllRecursively(TreeItem<FileNode> root) {
        try {
            for (var c : root.getChildren()) {
                if (!c.getValue().isDir() && c.getValue().getPath().isEmpty()) {
                    String fileName = c.getValue().getName();
                    File tempFile = File.createTempFile("extracted_", "_" + fileName);
                    tempFile.deleteOnExit();
                    extract(c.getValue().getId(), tempFile.getPath());
                    c.getValue().setFile(tempFile);
                }
                extractAllRecursively(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void extractDir(String path, TreeItem<FileNode> dirRoot) {
        try {
            extractDirRecursively(path, dirRoot);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void moveFileOrDir(String destPath, TreeItem<FileNode> dirRoot) {
        try {
            Files.copy(Path.of(dirRoot.getValue().getPath()), Path.of(destPath), REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void extractDirRecursively(String path, TreeItem<FileNode> dirRoot) throws IOException {
        if (dirRoot.getValue().isDir()) {
            var dir = new File(path);
            dir.mkdir();
        } else {
            if (dirRoot.getValue().isNew()) {
                moveFileOrDir(path, dirRoot);
            } else {
                extract(dirRoot.getValue().getId(), path);
            }
        }

        for (var child : dirRoot.getChildren()) {
            extractDirRecursively(path + "/" + child.getValue().getName(), child);
        }
    }

    public File extractToTemp(FileNode file) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("extracted_", "_" + file.getName());
            tempFile.deleteOnExit();
            System.out.println(tempFile.getPath());
            extract(file.getName(), tempFile.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tempFile;
    }
}
