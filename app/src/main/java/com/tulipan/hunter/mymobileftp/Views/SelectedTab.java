package com.tulipan.hunter.mymobileftp.Views;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;
import com.tulipan.hunter.mymobileftp.R;
import com.tulipan.hunter.mymobileftp.Structures.FTPClientWrapper;
import com.tulipan.hunter.mymobileftp.Structures.SelectedListAdapter;
import com.tulipan.hunter.mymobileftp.Structures.SelectedListItem;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Hunter on 12/14/2016.
 */
public class SelectedTab {
    private MyFTPActivity mParentActivity;
    private LinearLayout mButton;
    private LinearLayout mLayout;
    private FTPClientWrapper mClient;

    private ListView mServerListView;
    private ListView mClientListView;
    private SelectedListAdapter mClientListAdapter;
    private SelectedListAdapter mServerListAdapter;
    private ArrayList<SelectedListItem> mClientFileList;
    private ArrayList<SelectedListItem> mServerFileList;

    public SelectedTab(MyFTPActivity parent, LinearLayout button, LinearLayout layout) {
        mParentActivity = parent;
        mButton = button;
        mLayout = layout;
        mClient = mParentActivity.getClient();

        mClientListView = (ListView) layout.findViewById(R.id.selected_client_listview);
        mServerListView = (ListView) layout.findViewById(R.id.selected_server_listview);
        mClientFileList = new ArrayList<>();
        mServerFileList = new ArrayList<>();

        mClientListAdapter = new SelectedListAdapter(mParentActivity, R.layout.selected_file_list_row, mClientFileList, SelectedTab.this);
        mClientListView.setAdapter(mClientListAdapter);
        mClientListView.setTextFilterEnabled(true);

        mServerListAdapter = new SelectedListAdapter(mParentActivity, R.layout.selected_file_list_row, mServerFileList, SelectedTab.this);
        mServerListView.setAdapter(mServerListAdapter);
        mServerListView.setTextFilterEnabled(true);

        updateList();
    }

    public void updateList() {
        if (mClientFileList == null) mClientFileList = new ArrayList<>();
        if (mServerFileList == null) mServerFileList = new ArrayList<>();

        mClientFileList.clear();
        mServerFileList.clear();

        HashSet<String> clientList = mParentActivity.getClientFiles();
        HashSet<String> serverList = mParentActivity.getServerFiles();

        for (String s : clientList) {
            File f = new File(s);
            mClientFileList.add(new SelectedListItem(mParentActivity.getClientSelector(), f.getName(), f.getPath(), f.length(), true));
        }
        for (String s : serverList) {
            FTPFile f = mClient.getFileInfo(s);
            String[] parts = f.getName().split("/");
            String name = parts[parts.length-1];
            mServerFileList.add(new SelectedListItem(mParentActivity.getServerSelector(), name, f.getName(), f.getSize(), false));
        }
        mClientListAdapter.notifyDataSetChanged();
        mServerListAdapter.notifyDataSetChanged();
    }

    public void setSelected(boolean isSelected) {
        mButton.setSelected(isSelected);
        if (isSelected) {
            mLayout.setVisibility(View.VISIBLE);
        } else {
            mLayout.setVisibility(View.GONE);
        }
    }

    public void handToProgressTab(SelectedListItem item) {
        mParentActivity.addToTaskQueue(item);
    }
}
