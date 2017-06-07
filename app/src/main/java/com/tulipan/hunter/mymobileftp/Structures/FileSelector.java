package com.tulipan.hunter.mymobileftp.Structures;

import java.util.HashSet;

/**
 * Created by Hunter on 4/12/2017.
 */
public interface FileSelector {
    void navigateToFile(String path);
    void setItemSelected(String path, boolean selected);
    HashSet<String> getSelectedFiles();
    int getListItemLayout();
}
