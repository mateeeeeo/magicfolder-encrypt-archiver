package com.magicfolder.helpers;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class Folder {

    private String name;
    private String path;
    private String password;
    private List<File> files;

    public static final int KEY_AND_IV_LENGTH = 16;
    public static final int BCRYPT_HASH_LENGTH = 60;
    public static final int PASSWORD_DECORATOR_LENGTH = 1;

    public Folder(String name, String path, String password, List<File> files) {
        this.name = name;
        this.path = path;
        this.password = password;
        this.files = files;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Folder(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public void createArchive() {
        try {
            byte[] buffer = new byte[1024];
            FileOutputStream fos = new FileOutputStream(path);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] iv1 = new byte[16];
            byte[] iv2 = new byte[16];
            SecureRandom srandom = new SecureRandom();
            IvParameterSpec ivspec;
            KeyGenerator keygen = KeyGenerator.getInstance("AES");
            SecretKey skey1;
            SecretKey key2;
            Cipher cipher;

            // the '!'-decorator indicates to the program that the folder is password protected
            if (!password.isEmpty()) {
                fos.write("!".getBytes());
            }

            byte[] salt = BCrypt.gensalt().getBytes();
            fos.write((BCrypt.hashpw(password, new String(salt, StandardCharsets.UTF_8))).getBytes());

            skey1 = keygen.generateKey();

            srandom.nextBytes(iv1);

            KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, 10, 128);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            key2 = secretKeyFactory.generateSecret(keySpec);

            SecretKeySpec skey2 = new SecretKeySpec(key2.getEncoded(), "AES");
            srandom.nextBytes(iv2);

            ivspec = new IvParameterSpec(iv2);

            fos.write(iv2);

            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skey2, ivspec);

            baos.write(skey1.getEncoded());
            baos.write(iv1);
            baos.close();

            fos.write(cipher.doFinal(baos.toByteArray()));

            ivspec = new IvParameterSpec(iv1);

            cipher.init(Cipher.ENCRYPT_MODE, skey1, ivspec);

            // creates the file dictionary, where filenames and file sizes are stored
            for (File file : files) {
                fos.write(Base64.getEncoder().encode(cipher.doFinal(file.getName().getBytes())));
                fos.write("\t".getBytes());
                fos.write(Base64.getEncoder().encode(cipher.doFinal(Long.toString(file.length()).getBytes())));
                fos.write("\n".getBytes());
            }

            // dictionary separator
            fos.write("\f".getBytes());

            int len;
            FileInputStream fis = null;
            // writes the encrypted contents of all included files to the folder
            for (File file : files) {
                fis = new FileInputStream(file.getPath());

                while ((len = fis.read(buffer)) != -1) {
                    fos.write(cipher.update(buffer, 0, len));
                }

                fos.write(cipher.doFinal());
            }

            if (fis != null) {
                fis.close();
            }
            fos.close();

        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fis - the FileInputStream object to be used
     * @param cipher - the Cipher object to be used to decrypt the encrypted key and iv pair
     * @param ivSpec - the IvParameterSpec to be used decrypt the encrypted key and iv pair
     * @param skey - the SecretKeySpec to be used to decrypt the encrypted key and iv pair
     * @return a 2d-byte array containing firstly the key and secondly the iv used to encrypt the folder
     *
     * below we look for the key & iv pair with which the files have been encrypted
     * this pair is stored in an encrypted form in the file itself, the encrypted sequence being padded, therefore
     * we calculate the padding size for the decryption process
     * the input size is (2 * 16), since we're looking for a key & iv pair, both being 16 bytes long
     */

    private byte[][] getFolderKeyAndIv(FileInputStream fis, Cipher cipher, SecretKeySpec skey, IvParameterSpec ivSpec) throws IOException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] buffer = new byte[(int) FolderTools.getPaddedOutputSize(2 * 16, cipher.getBlockSize())];
        byte[] keyIv;

        /* 1 byte ('!'-decorator) + 60 bytes (bcrypt hash) + 16 bytes () */
        fis.skip(77);
        fis.read(buffer);

        cipher.init(Cipher.DECRYPT_MODE, skey, ivSpec);
        keyIv = cipher.doFinal(buffer);

        byte[] outKey = Arrays.copyOfRange(keyIv, 0, 16);
        byte[] outIv = Arrays.copyOfRange(keyIv, 16, 32);

        return new byte[][] { outKey, outIv };
    }

    public ArrayList<String> getFileNames(byte[] key2, byte[] iv2) {
        ArrayList<String> fileNames = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(path);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec skey2 = new SecretKeySpec(key2, "AES"); // the key used to encrypt the key used to encrypt the file contents
            IvParameterSpec ivSpec2 = new IvParameterSpec(iv2); // the iv used to encrypt the key used to encrypt the file contents
            String fileName;

            byte[][] keyAndIv = getFolderKeyAndIv(fis, cipher, skey2, ivSpec2);
            byte[] folderKey = keyAndIv[0];
            byte[] folderIv = keyAndIv[1];

            SecretKeySpec skey1 = new SecretKeySpec(folderKey, "AES");
            IvParameterSpec ivSpec1 = new IvParameterSpec(folderIv);
            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec1);

            byte[] buffer = new byte[1024];
            fis.read(buffer);
            if (ArrayTools.contains(buffer, "\n".getBytes())) {
                if (ArrayTools.indexOf(buffer, "\n".getBytes()) < ArrayTools.indexOf(buffer, "\t".getBytes())) {
                    buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
                }

                while (buffer[0] != (byte) '\f') {
                    if (ArrayTools.contains(buffer, "\t".getBytes()) && ArrayTools.indexOf(buffer, "\n".getBytes()) > ArrayTools.indexOf(buffer, "\t".getBytes())) {

                        fileNames.add(new String(cipher.doFinal(Base64.getDecoder().decode(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\t".getBytes()))))));
                        buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);

                    } else if (!ArrayTools.contains(buffer, "\t".getBytes())) {
                        fileName = new String(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length()));
                        fis.read(buffer);
                        buffer = cipher.update(buffer);
                        fileName += new String(cipher.doFinal(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\t".getBytes()))));
                        fileNames.add(fileName);
                        buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);

                    }
                }
            }

            fis.close();

        } catch (
                Exception e) {
            e.printStackTrace();
        }

        return fileNames;
    }

//    public ArrayList<String> getFileNames(byte[] key2, byte[] iv2) {
//        ArrayList<String> fileNames = new ArrayList<>();
//
//        try {
//            FileInputStream fis = new FileInputStream(new File(path));
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            SecretKeySpec skey2 = new SecretKeySpec(key2, "AES");
//            IvParameterSpec ivSpec = new IvParameterSpec(iv2);
//            String fileName;
//
//            byte[] buffer = new byte[48];
//            byte[] keyIv;
//            fis.skip(77);
//            fis.read(buffer);
//
//            cipher.init(Cipher.DECRYPT_MODE, skey2, ivSpec);
//
//            keyIv = cipher.doFinal(buffer);
//            byte[] key1 = Arrays.copyOfRange(keyIv, 0, 16);
//
//            SecretKeySpec skey1 = new SecretKeySpec(key1, "AES");
//
//            byte[] iv1 = Arrays.copyOfRange(keyIv, 16, 32);
//
//            ivSpec = new IvParameterSpec(iv1);
//
//            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
//
////            long elapsedBytes = 125;
//
////            if (new File(path).length() - elapsedBytes > 1024) {
////
////                buffer = new byte[1024];
////
////            } else {
////
////                buffer = new byte[(int) (new File(path).length() - elapsedBytes)];
////
////            }
//
//            buffer = new byte[1024];
//            fis.read(buffer);
//            if (ArrayTools.contains(buffer, "\n".getBytes())) {
//                if (ArrayTools.indexOf(buffer, "\n".getBytes()) < ArrayTools.indexOf(buffer, "\t".getBytes())) {
//
//                    buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
//
//                }
//
//                while (buffer[0] != (byte) '\f') {
//
//                    if (ArrayTools.contains(buffer, "\t".getBytes()) && ArrayTools.indexOf(buffer, "\n".getBytes()) > ArrayTools.indexOf(buffer, "\t".getBytes())) {
//
//                        fileNames.add(new String(cipher.doFinal(Base64.getDecoder().decode(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\t".getBytes()))))));
//                        buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
//
//                    } else if (!ArrayTools.contains(buffer, "\t".getBytes())) {
//                        fileName = new String(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length()));
//                        fis.read(buffer);
//                        buffer = cipher.update(buffer);
//                        fileName += new String(cipher.doFinal(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\t".getBytes()))));
//                        fileNames.add(fileName);
//                        buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
//
//                    }
//                }
//            }
//
//            fis.close();
//
//        } catch (
//                Exception e) {
//            e.printStackTrace();
//        }
//
//        return fileNames;
//    }

    public ArrayList<Long> getFileSizes(byte[] key2, byte[] iv2) {

        ArrayList<Long> fileSizes = new ArrayList<>();

        try {
            FileInputStream fis = new FileInputStream(path);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec skey2 = new SecretKeySpec(key2, "AES");
            IvParameterSpec ivSpec2 = new IvParameterSpec(iv2);

            byte[][] keyAndIv = getFolderKeyAndIv(fis, cipher, skey2, ivSpec2);
            byte[] folderKey = keyAndIv[0];
            byte[] folderIv = keyAndIv[1];

            SecretKeySpec skey1 = new SecretKeySpec(folderKey, "AES");
            IvParameterSpec ivSpec1 = new IvParameterSpec(folderIv);
            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec1);

            byte[] buffer;

            // the size of the buffer is adjusted based on the encrypted contents (subtracting 125, made up of the password hash and the encrypted key & iv pair)
            if (new File(path).length() - 125 > 1024) {
                buffer = new byte[1024]; // a regular sized buffer is used
            } else {
                buffer = new byte[(int) (new File(path).length() - 125)]; // a buffer that has just enough space for the encrypted contents of the folder
            }

            fis.read(buffer);

            String fileSizeString;
            while (buffer[0] != (byte) '\f') {
                if (ArrayTools.indexOf(buffer, "\n".getBytes()) > ArrayTools.indexOf(buffer, "\t".getBytes())) {
                    fileSizes.add(Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\t".getBytes()) + "\t".length(), ArrayTools.indexOf(buffer, "\n".getBytes())))))));
                    buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
                } else {
                    fileSizeString = new String(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\t".getBytes()) + "\t".length()));
                    fis.read(buffer);
                    buffer = cipher.update(buffer);
                    fileSizeString += new String(cipher.doFinal(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\n".getBytes()))));
                    fileSizes.add(Long.parseLong(fileSizeString));
                    buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
                }
            }
            fis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileSizes;
    }

//    public ArrayList<Long> getFileSizes(byte[] key2, byte[] iv2) {
//
//        ArrayList<Long> fileSizes = new ArrayList<>();
//
//        try {
//            FileInputStream fis = new FileInputStream(path);
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            SecretKeySpec skey2 = new SecretKeySpec(key2, "AES");
//            IvParameterSpec ivSpec2 = new IvParameterSpec(iv2);
//
//            byte[][] keyAndIv = getFolderKeyAndIv(fis, cipher, skey2, ivSpec2);
//            byte[] folderKey = keyAndIv[0];
//            byte[] folderIv = keyAndIv[1];
//
//            SecretKeySpec skey1 = new SecretKeySpec(folderKey, "AES");
//            IvParameterSpec ivSpec1 = new IvParameterSpec(folderIv);
//            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec1);
//
//            byte[] buffer;
//
//            // the size of the buffer is adjusted based on the encrypted contents (subtracting 125, made up of the password hash and the encrypted key & iv pair)
//            if (new File(path).length() - 125 > 1024) {
//                buffer = new byte[1024]; // a regular sized buffer is used
//            } else {
//                buffer = new byte[(int) (new File(path).length() - 125)]; // a buffer that has just enough space for the encrypted contents of the folder
//            }
//
//            fis.read(buffer);
//
//            String fileSizeString;
//            while (buffer[0] != (byte) '\f') {
//                if (ArrayTools.indexOf(buffer, "\n".getBytes()) > ArrayTools.indexOf(buffer, "\t".getBytes())) {
//                    fileSizes.add(Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\t".getBytes()) + "\t".length(), ArrayTools.indexOf(buffer, "\n".getBytes())))))));
//                    buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
//                } else {
//                    fileSizeString = new String(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\t".getBytes()) + "\t".length()));
//                    fis.read(buffer);
//                    buffer = cipher.update(buffer);
//                    fileSizeString += new String(cipher.doFinal(Arrays.copyOfRange(buffer, 0, ArrayTools.indexOf(buffer, "\n".getBytes()))));
//                    fileSizes.add(Long.parseLong(fileSizeString));
//                    buffer = Arrays.copyOfRange(buffer, ArrayTools.indexOf(buffer, "\n".getBytes()) + "\n".length(), buffer.length);
//                }
//            }
//            fis.close();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return fileSizes;
//    }

    public void extract(String fileName, byte[] key2, byte[] iv2) {
        try {
            FileInputStream fis = new FileInputStream(path);

            String extractPath = path.replace(name, fileName);

            if (new File(extractPath).exists()) {
                final int extensionIndex = extractPath.lastIndexOf('.');

                StringBuilder sb = new StringBuilder(extractPath);
                sb.insert(extensionIndex, "(1)");

                extractPath = sb.toString();
            }

            FileOutputStream fos = new FileOutputStream(extractPath);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec skey2 = new SecretKeySpec(key2, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv2);

            byte[] buffer = new byte[48];
            byte[] keyIv;

            fis.skip(77);
            fis.read(buffer);

            cipher.init(Cipher.DECRYPT_MODE, skey2, ivSpec);

            keyIv = cipher.doFinal(buffer);
            byte[] key1 = Arrays.copyOfRange(keyIv, 0, 16);

            SecretKeySpec skey1 = new SecretKeySpec(key1, "AES");

            byte[] iv1 = Arrays.copyOfRange(keyIv, 16, 32);

            ivSpec = new IvParameterSpec(iv1);


            int len;
            long fileSize = 0;
            buffer = new byte[1024];

            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);

            fis = new FileInputStream(new File(path));

            long dictionarySize = 0;


            //CREATES DICTIONARY

            byte[] dictionary = null;

            while (fis.read(buffer) != -1) {

                dictionary = Arrays.copyOf(buffer, buffer.length);

                if (ArrayTools.contains(dictionary, "\f".getBytes())) {

                    dictionary = Arrays.copyOfRange(buffer, 125, ArrayTools.indexOf(Arrays.copyOfRange(dictionary, 125, dictionary.length), "\n\f".getBytes()) + "\n\f".length() + 125);
                    dictionarySize += dictionary.length;
                    break;

                } else {

                    baos.write(dictionary);
                    fis.read(buffer);
                    baos.write(buffer);
                    dictionary = baos.toByteArray();
                    baos.reset();

                }
            }

            //GETS SIZE OF FILE TO BE EXTRACTED

            cipher.init(Cipher.ENCRYPT_MODE, skey1, ivSpec);

            byte[] tempDictionary = Arrays.copyOf(dictionary, dictionary.length);

            if (ArrayTools.contains(
                    tempDictionary,
                    Base64.getEncoder()
                            .encode(
                                    cipher.doFinal(
                                            fileName.getBytes()
                                    )))) {

                tempDictionary = Arrays.copyOfRange(
                        tempDictionary,
                        ArrayTools
                                .indexOf(tempDictionary,
                                        Base64.getEncoder()
                                                .encode(
                                                        cipher.doFinal(
                                                                fileName.getBytes()))) + (fileName + "\t").length()
                        , tempDictionary.length);
                cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
                fileSize = Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(Arrays.copyOfRange(tempDictionary, ArrayTools.indexOf(tempDictionary, "\t".getBytes()) + "\t".length(), ArrayTools.indexOf(tempDictionary, "\n".getBytes()))))));

            }

            long bytesToSkip = 0;
            tempDictionary = Arrays.copyOf(dictionary, dictionary.length);
            byte[] fileSlot;

            //CACLULATES BYTES TO BE SKIPPED.

            while (ArrayTools.contains(tempDictionary, "\n".getBytes()) && ArrayTools.contains(tempDictionary, "\t".getBytes())) {
                if (ArrayTools.indexOf(tempDictionary, "\t".getBytes()) < ArrayTools.indexOf(tempDictionary, "\n".getBytes())) {
                    fileSlot = Arrays.copyOfRange(tempDictionary, 0, ArrayTools.indexOf(tempDictionary, "\n".getBytes()));
                    tempDictionary = Arrays.copyOfRange(tempDictionary, ArrayTools.indexOf(tempDictionary, "\n".getBytes()) + "\n".length(), tempDictionary.length);
                    cipher.init(Cipher.ENCRYPT_MODE, skey1, ivSpec);

                    if (!ArrayTools.contains(fileSlot, Base64.getEncoder().encode(cipher.doFinal(fileName.getBytes())))) {
                        cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
                        bytesToSkip += FolderTools.getPaddedOutputSize(Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(
                                Arrays.copyOfRange(fileSlot, ArrayTools.indexOf(fileSlot, "\t".getBytes()) + "\t".length(), fileSlot.length))))),
                                cipher.getBlockSize());

                    } else {

                        break;

                    }

                } else {
                    break;
                }
            }

            long elapsedBytes = 0;
            bytesToSkip += dictionarySize + 125;
            fis = new FileInputStream(new File(path));
            fis.skip(bytesToSkip);

            //DECRYPTS FILE

            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);

            while ((len = fis.read(buffer)) != -1) {

                if (FolderTools.getPaddedOutputSize(fileSize, cipher.getBlockSize()) - elapsedBytes > 1024) {
                    fos.write(cipher.update(buffer, 0, len));
                    elapsedBytes += len;

                } else if (len > fileSize - elapsedBytes) {

                    buffer = Arrays.copyOf(buffer, (int) FolderTools.getPaddedOutputSize(fileSize - elapsedBytes, cipher.getBlockSize()));
                    System.out.println(buffer.length);
                    fos.write(cipher.doFinal(buffer));
                    System.out.println((int) (fileSize - elapsedBytes));
                    break;

                } else {
                    System.out.println("len < fileSize - elapsedBytes");
                    fos.write(cipher.update(buffer, 0, len));
                    elapsedBytes += len;
                }
            }

            fis.close();
            fos.close();
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void extract(String fileName, String extractPath, byte[] key2, byte[] iv2) {
        try {
            String extractedFilePath = extractPath + "/" + fileName;
            FileInputStream fis = new FileInputStream(path);

//            if (new File(extractedFilePath).exists()) {
//                final int extensionIndex = extractedFilePath.lastIndexOf('.');
//
//                StringBuilder sb = new StringBuilder(extractedFilePath);
//                sb.insert(extensionIndex, "(1)");
//
//                extractedFilePath = sb.toString();
//            }

            FileOutputStream fos = new FileOutputStream(extractedFilePath);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec skey2 = new SecretKeySpec(key2, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv2);

            byte[] buffer = new byte[48];
            byte[] keyIv;

            fis.skip(77);
            fis.read(buffer);

            cipher.init(Cipher.DECRYPT_MODE, skey2, ivSpec);

            keyIv = cipher.doFinal(buffer);
            byte[] key1 = Arrays.copyOfRange(keyIv, 0, 16);

            SecretKeySpec skey1 = new SecretKeySpec(key1, "AES");

            byte[] iv1 = Arrays.copyOfRange(keyIv, 16, 32);

            ivSpec = new IvParameterSpec(iv1);

            long fileSize = 0;
            buffer = new byte[1024];

            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);

            fis = new FileInputStream(path);

            long dictionarySize = 0;

            //CREATES DICTIONARY

            byte[] dictionary = null;

            while (fis.read(buffer) != -1) {

                dictionary = Arrays.copyOf(buffer, buffer.length);

                if (ArrayTools.contains(dictionary, "\f".getBytes())) {
                    dictionary = Arrays.copyOfRange(buffer, 125, ArrayTools.indexOf(Arrays.copyOfRange(dictionary, 125, dictionary.length), "\n\f".getBytes()) + "\n\f".length() + 125);
                    dictionarySize += dictionary.length;
                    break;
                } else {
                    baos.write(dictionary);
                    fis.read(buffer);
                    baos.write(buffer);
                    dictionary = baos.toByteArray();
                    baos.reset();

                }
            }

            //GETS SIZE OF FILE TO BE EXTRACTED

            cipher.init(Cipher.ENCRYPT_MODE, skey1, ivSpec);

            byte[] tempDictionary = Arrays.copyOf(dictionary, dictionary.length);

            if (ArrayTools.contains(tempDictionary, Base64.getEncoder().encode(cipher.doFinal(fileName.getBytes())))) {
                tempDictionary = Arrays.copyOfRange(tempDictionary, ArrayTools.indexOf(tempDictionary, Base64.getEncoder().encode(cipher.doFinal(fileName.getBytes()))) + (fileName + "\t").length(), tempDictionary.length);
                cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
                fileSize = Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(Arrays.copyOfRange(tempDictionary, ArrayTools.indexOf(tempDictionary, "\t".getBytes()) + "\t".length(), ArrayTools.indexOf(tempDictionary, "\n".getBytes()))))));
            }

            long bytesToSkip = 0;
            tempDictionary = Arrays.copyOf(dictionary, dictionary.length);
            byte[] fileSlot;

            //CACLULATES BYTES TO BE SKIPPED.

            while (ArrayTools.contains(tempDictionary, "\n".getBytes()) && ArrayTools.contains(tempDictionary, "\t".getBytes())) {
                if (ArrayTools.indexOf(tempDictionary, "\t".getBytes()) < ArrayTools.indexOf(tempDictionary, "\n".getBytes())) {
                    fileSlot = Arrays.copyOfRange(tempDictionary, 0, ArrayTools.indexOf(tempDictionary, "\n".getBytes()));
                    tempDictionary = Arrays.copyOfRange(tempDictionary, ArrayTools.indexOf(tempDictionary, "\n".getBytes()) + "\n".length(), tempDictionary.length);
                    cipher.init(Cipher.ENCRYPT_MODE, skey1, ivSpec);

                    if (!ArrayTools.contains(fileSlot, Base64.getEncoder().encode(cipher.doFinal(fileName.getBytes())))) {
                        cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
                        bytesToSkip += FolderTools.getPaddedOutputSize(Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(
                                Arrays.copyOfRange(fileSlot, ArrayTools.indexOf(fileSlot, "\t".getBytes()) + "\t".length(), fileSlot.length))))),
                                cipher.getBlockSize());

                    } else {

                        break;

                    }

                } else {
                    break;
                }
            }

            long elapsedBytes = 0;
            bytesToSkip += dictionarySize + 125;
            fis = new FileInputStream(path);
            fis.skip(bytesToSkip);

            //DECRYPTS FILE

            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);

            int len;
            while ((len = fis.read(buffer)) != -1) {
                if (FolderTools.getPaddedOutputSize(fileSize, cipher.getBlockSize()) - elapsedBytes > 1024) {
                    fos.write(cipher.update(buffer, 0, len));
                    elapsedBytes += len;

                } else if (len > fileSize - elapsedBytes) {

                    buffer = Arrays.copyOf(buffer, (int) FolderTools.getPaddedOutputSize(fileSize - elapsedBytes, cipher.getBlockSize()));
                    System.out.println(buffer.length);
                    fos.write(cipher.doFinal(buffer));
                    System.out.println((int) (fileSize - elapsedBytes));
                    break;

                } else {
                    System.out.println("len < fileSize - elapsedBytes");
                    fos.write(cipher.update(buffer, 0, len));
                    elapsedBytes += len;
                }
            }

            fis.close();
            fos.close();
            System.out.println("done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void extract(String fileName, String extractPath, byte[] key2, byte[] iv2) {
//        try {
//
//            String extractedFilePath = extractPath + "/" + fileName;
//            FileInputStream fis = new FileInputStream(path);
//
//            if (new File(extractedFilePath).exists()) {
//                final int extensionIndex = extractedFilePath.lastIndexOf('.');
//
//                StringBuilder sb = new StringBuilder(extractedFilePath);
//                sb.insert(extensionIndex, "(1)");
//
//                extractedFilePath = sb.toString();
//            }
//
//            FileOutputStream fos = new FileOutputStream(extractedFilePath);
//
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            SecretKeySpec skey2 = new SecretKeySpec(key2, "AES");
//            IvParameterSpec ivSpec = new IvParameterSpec(iv2);
//
//            byte[] buffer = new byte[48];
//            byte[] keyIv;
//
//            fis.skip(77);
//            fis.read(buffer);
//
//            cipher.init(Cipher.DECRYPT_MODE, skey2, ivSpec);
//
//            keyIv = cipher.doFinal(buffer);
//            byte[] key1 = Arrays.copyOfRange(keyIv, 0, 16);
//
//            SecretKeySpec skey1 = new SecretKeySpec(key1, "AES");
//
//            byte[] iv1 = Arrays.copyOfRange(keyIv, 16, 32);
//
//            ivSpec = new IvParameterSpec(iv1);
//
//
//            int len;
//            long fileSize = 0;
//            buffer = new byte[1024];
//
//            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
//
//            fis = new FileInputStream(new File(path));
//
//            long dictionarySize = 0;
//
//
//            //CREATES DICTIONARY
//
//            byte[] dictionary = null;
//
//            while (fis.read(buffer) != -1) {
//
//                dictionary = Arrays.copyOf(buffer, buffer.length);
//
//                if (ArrayTools.contains(dictionary, "\f".getBytes())) {
//
//                    dictionary = Arrays.copyOfRange(buffer, 125, ArrayTools.indexOf(Arrays.copyOfRange(dictionary, 125, dictionary.length), "\n\f".getBytes()) + "\n\f".length() + 125);
//                    dictionarySize += dictionary.length;
//                    break;
//
//                } else {
//
//                    baos.write(dictionary);
//                    fis.read(buffer);
//                    baos.write(buffer);
//                    dictionary = baos.toByteArray();
//                    baos.reset();
//
//                }
//            }
//
//            //GETS SIZE OF FILE TO BE EXTRACTED
//
//            cipher.init(Cipher.ENCRYPT_MODE, skey1, ivSpec);
//
//            byte[] tempDictionary = Arrays.copyOf(dictionary, dictionary.length);
//
//            if (ArrayTools.contains(
//                    tempDictionary,
//                    Base64.getEncoder()
//                            .encode(
//                                    cipher.doFinal(
//                                            fileName.getBytes()
//                                    )))) {
//
//                tempDictionary = Arrays.copyOfRange(
//                        tempDictionary,
//                        ArrayTools
//                                .indexOf(tempDictionary,
//                                        Base64.getEncoder()
//                                                .encode(
//                                                        cipher.doFinal(
//                                                                fileName.getBytes()))) + (fileName + "\t").length()
//                        , tempDictionary.length);
//                cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
//                fileSize = Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(Arrays.copyOfRange(tempDictionary, ArrayTools.indexOf(tempDictionary, "\t".getBytes()) + "\t".length(), ArrayTools.indexOf(tempDictionary, "\n".getBytes()))))));
//
//            }
//
//            long bytesToSkip = 0;
//            tempDictionary = Arrays.copyOf(dictionary, dictionary.length);
//            byte[] fileSlot;
//
//            //CACLULATES BYTES TO BE SKIPPED.
//
//            while (ArrayTools.contains(tempDictionary, "\n".getBytes()) && ArrayTools.contains(tempDictionary, "\t".getBytes())) {
//                if (ArrayTools.indexOf(tempDictionary, "\t".getBytes()) < ArrayTools.indexOf(tempDictionary, "\n".getBytes())) {
//                    fileSlot = Arrays.copyOfRange(tempDictionary, 0, ArrayTools.indexOf(tempDictionary, "\n".getBytes()));
//                    tempDictionary = Arrays.copyOfRange(tempDictionary, ArrayTools.indexOf(tempDictionary, "\n".getBytes()) + "\n".length(), tempDictionary.length);
//                    cipher.init(Cipher.ENCRYPT_MODE, skey1, ivSpec);
//
//                    if (!ArrayTools.contains(fileSlot, Base64.getEncoder().encode(cipher.doFinal(fileName.getBytes())))) {
//                        cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
//                        bytesToSkip += FolderTools.getEncryptedOutputSize(Long.parseLong(new String(cipher.doFinal(Base64.getDecoder().decode(
//                                Arrays.copyOfRange(fileSlot, ArrayTools.indexOf(fileSlot, "\t".getBytes()) + "\t".length(), fileSlot.length))))),
//                                cipher.getBlockSize());
//
//                    } else {
//
//                        break;
//
//                    }
//
//                } else {
//                    break;
//                }
//            }
//
//            long elapsedBytes = 0;
//            bytesToSkip += dictionarySize + 125;
//            fis = new FileInputStream(new File(path));
//            fis.skip(bytesToSkip);
//
//            //DECRYPTS FILE
//
//            cipher.init(Cipher.DECRYPT_MODE, skey1, ivSpec);
//
//            while ((len = fis.read(buffer)) != -1) {
//
//                if (FolderTools.getEncryptedOutputSize(fileSize, cipher.getBlockSize()) - elapsedBytes > 1024) {
//                    fos.write(cipher.update(buffer, 0, len));
//                    elapsedBytes += len;
//
//                } else if (len > fileSize - elapsedBytes) {
//
//                    buffer = Arrays.copyOf(buffer, (int) FolderTools.getEncryptedOutputSize(fileSize - elapsedBytes, cipher.getBlockSize()));
//                    System.out.println(buffer.length);
//                    fos.write(cipher.doFinal(buffer));
//                    System.out.println((int) (fileSize - elapsedBytes));
//                    break;
//
//                } else {
//                    System.out.println("len < fileSize - elapsedBytes");
//                    fos.write(cipher.update(buffer, 0, len));
//                    elapsedBytes += len;
//                }
//            }
//
//            fis.close();
//            fos.close();
//            System.out.println("done");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
