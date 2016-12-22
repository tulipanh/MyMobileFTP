package com.tulipan.hunter.mymobileftp.Views;

import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;

/**
 * Created by Hunter on 12/14/2016.
 */
public class ProgressTab {
    private MyFTPActivity mParentActivity;
    private Button mButton;
    private ListView mListView;

    public ProgressTab(MyFTPActivity parent, Button button, ListView list) {
        mParentActivity = parent;
        mButton = button;
        mListView = list;

        //Set up the list or recycler view here.
    }

    public void setSelected(boolean isSelected) {
        mButton.setSelected(isSelected);
        if (isSelected) {
            mListView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.GONE);
        }
    }
}
