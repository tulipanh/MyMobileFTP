package com.tulipan.hunter.mymobileftp.Structures;

/**
 * Created by Hunter on 4/27/2017.
 */
public class TransferItem {
    public String fileName;
    public String filePath;
    public long fileSize;
    public int fileLocation;
    public String targetPath;
    public int transferProgress;
    public int transferResult;

    public TransferItem(String name, String path, long size, String t, int l, int progress, int result) {
        fileName = name;
        filePath = path;
        fileSize = size;
        targetPath = t;
        fileLocation = l;
        transferProgress = progress;
        transferResult = result;
    }

    public boolean isClient() {
        return fileLocation == TransferManager.CLIENT;
    }
}
