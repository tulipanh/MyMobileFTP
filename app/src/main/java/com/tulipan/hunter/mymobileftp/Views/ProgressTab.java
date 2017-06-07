package com.tulipan.hunter.mymobileftp.Views;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;
import com.tulipan.hunter.mymobileftp.R;
import com.tulipan.hunter.mymobileftp.Structures.ProgressListAdapter;
import com.tulipan.hunter.mymobileftp.Structures.TransferItem;

import java.util.List;

/**
 * Created by Hunter on 12/14/2016.
 */
public class ProgressTab {
    private MyFTPActivity mParentActivity;
    private LinearLayout mButton;
    private ListView mListView;
    private boolean mPaused;
    private List<TransferItem> mTransferList;

    private ProgressListAdapter mAdapter;


    public ProgressTab(MyFTPActivity parent, LinearLayout button, ListView list) {
        mParentActivity = parent;
        mButton = button;
        mListView = list;
        mTransferList = mParentActivity.getTransferList();

        mAdapter = new ProgressListAdapter(mParentActivity, R.layout.progress_list_row, mTransferList, ProgressTab.this);
        mListView.setAdapter(mAdapter);
        mListView.setTextFilterEnabled(true);

        mPaused = false;

        updateList();
    }

    public void updateList() {
        mTransferList = mParentActivity.getTransferList();
        mAdapter.notifyDataSetChanged();
    }

    public void setSelected(boolean isSelected) {
        mButton.setSelected(isSelected);
        if (isSelected) {
            mListView.setVisibility(View.VISIBLE);
        } else {
            mListView.setVisibility(View.GONE);
        }
    }

    public void callResumeTransfer() {
        mParentActivity.resumeTransfer();
    }

    public void callPauseTransfer() {
        mParentActivity.pauseTransfer();
    }

    public boolean checkIfPaused() {
        return mParentActivity.checkIfTransferPaused();
    }

    public void removeProgressItem(TransferItem item) {
        mParentActivity.removeItem(item);
    }
}
