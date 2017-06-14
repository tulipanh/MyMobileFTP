package com.tulipan.hunter.mymobileftp.Structures;

import java.util.HashSet;

public interface FileSelector {
    void navigateToFile(String path);
    void setItemSelected(String path, boolean selected);
    HashSet<String> getSelectedFiles();
    int getListItemLayout();
}
