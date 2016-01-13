package com.speakr.connorriley.speakr;

import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by connorriley on 1/13/16.
 */
public class WiFiDirectBundle implements Serializable {
    private String fileName;
    private String mimeType;
    private Long fileSize;
    private byte[] fileContent;

    public WiFiDirectBundle() {}

    // adds a file to the bundle, given its URI
    public void setFile(Uri uri) {
        File f = new File(String.valueOf(Uri.parse(uri.toString())));

        fileName = f.getName();
        mimeType = MimeTypeMap.getFileExtensionFromUrl(f.getAbsolutePath());
        fileSize = f.length();

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        fileContent = new byte[(int) f.length()];
        try {
            fin.read(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // restores the file of the bundle, given its directory (change to whatever
    // fits you better)
    public String restoreFile(String baseDir) {
        File f = new File(baseDir + "/" + fileName);
        try {
            FileOutputStream fos = new FileOutputStream(f);
            if (fileContent != null) {
                fos.write(fileContent);
            }

            fos.close();
            return f.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }
}