package com.tulipan.hunter.mymobileftp.Structures;

/**
 * Created by Hunter on 4/10/2017.
 */
public class FileListItem {
    private String mFileName;
    private String mFilePath;
    private boolean mSelected;
    private boolean mDirectory;

    public FileListItem(String name, String path, boolean isDir) {
        mFileName = name;
        mFilePath = path;
        mDirectory = isDir;
        mSelected = false;
    }

    public String getName() {return mFileName;}
    public String getPath() {return mFilePath;}
    public boolean isSelected() {return mSelected;}
    public boolean isDirectory() {return mDirectory;}

    public void setSelected(boolean value) {mSelected = value;}
}
