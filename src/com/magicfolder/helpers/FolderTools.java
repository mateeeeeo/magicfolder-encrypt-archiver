package com.magicfolder.helpers;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

public class FolderTools {

    public static long getPaddedOutputSize(long inputSize, int blockSize) {
        return inputSize + (blockSize - (inputSize % blockSize));
    }
}
