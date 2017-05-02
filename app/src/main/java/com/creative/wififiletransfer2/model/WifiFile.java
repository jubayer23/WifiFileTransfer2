package com.creative.wififiletransfer2.model;

import java.io.Serializable;

/**
 * Created by comsol on 02-May-17.
 */
public class WifiFile implements Serializable{

    String fileName;
    long fileLength;

    public WifiFile(String fileName, long fileLength) {
        this.fileName = fileName;
        this.fileLength = fileLength;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(long fileLength) {
        this.fileLength = fileLength;
    }
}
