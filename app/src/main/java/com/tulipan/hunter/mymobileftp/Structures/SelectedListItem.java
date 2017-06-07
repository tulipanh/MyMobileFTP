package com.tulipan.hunter.mymobileftp.Structures;

import android.support.v4.app.Fragment;

/**
 * Created by Hunter on 4/11/2017.
 */
public class SelectedListItem {
    private String mFileName;
    private String mFilePath;
    private boolean mClient;
    private long mSize;
    private FileSelector mParent;

    public SelectedListItem(FileSelector parent, String name, String path, long size, boolean isClient) {
        mFileName = name;
        mFilePath = path;
        mSize = size;
        mClient = isClient;
        mParent = parent;
    }

    public String getName() {return mFileName;}
    public String getPath() {return mFilePath;}
    public boolean isClient() {return mClient;}
    public long getSize() {return mSize;}

    public void setClient(boolean value) {mClient = value;}

    public void deselect() {
        mParent.setItemSelected(mFilePath, false);
    }


}
