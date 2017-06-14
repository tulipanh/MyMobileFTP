package com.tulipan.hunter.mymobileftp.Structures;

public class SelectedListItem {
    private String mFileName;
    private String mFilePath;
    private boolean mClient;
    private long mSize;
    private FileSelector mParent;

    public SelectedListItem(FileSelector parent, String name, String path,
                            long size, boolean isClient) {
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
