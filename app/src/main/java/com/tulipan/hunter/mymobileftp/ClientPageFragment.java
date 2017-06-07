package com.tulipan.hunter.mymobileftp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.Structures.FTPClientWrapper;
import com.tulipan.hunter.mymobileftp.Structures.FileExplorer;
import com.tulipan.hunter.mymobileftp.Structures.FileListAdapter;
import com.tulipan.hunter.mymobileftp.Structures.FileListItem;
import com.tulipan.hunter.mymobileftp.Structures.FileSelector;

import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Hunter on 1/17/2017.
 */
public class ClientPageFragment extends Fragment implements FileSelector {
    private MyFTPActivity mCurrentActivity;
    private FTPClientWrapper mClient;
    private File mCurrentDirectory;
    private File mHomeDirectory;
    private FileExplorer mExplorer;
    private HashSet<String> mSelectedList;
    private TextView mCurrentTextView = null;

    private ListView mListView;
    private FileListAdapter mListAdapter;
    private ArrayList<FileListItem> mFileList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = (MyFTPActivity) getActivity();
        mHomeDirectory = new File("/");
        initFragment();

        mClient = mCurrentActivity.getClient();
    }

    private void initFragment() {
        if (mCurrentDirectory == null) {
            mCurrentDirectory = new File(mHomeDirectory.getPath());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.client_page_fragment, container, false);

        initInterface(v);
        updateList();

        return v;
    }

    private void initInterface(View view) {
        mListView = (ListView) view.findViewById(R.id.client_explorer_listview);
        if (mFileList == null) mFileList = new ArrayList<>();
        if (mSelectedList == null) mSelectedList = new HashSet<>();

        mCurrentTextView = (TextView) view.findViewById(R.id.client_working_directory);
        mCurrentTextView.setText(mCurrentDirectory.getPath());

        if (mExplorer == null) {
            mExplorer = new FileExplorer(mCurrentActivity);
            mExplorer.setCurrentDirectory(mHomeDirectory);
        }

        if (mListAdapter == null) mListAdapter = new FileListAdapter(mCurrentActivity, R.layout.file_list_row_red, mFileList, ClientPageFragment.this);
        mListView.setAdapter(mListAdapter);
        mListView.setTextFilterEnabled(true);
    }

    public void reset() {
        // TODO: This method requires the existence of mExplorer, mCurrentActivity, mHomeDirectory, mCurrentDirectory, mCurrentTextView, so it might be prudent to test for the existence of all of these.
        if (mCurrentActivity != null) navigateToFile(mHomeDirectory.getPath());
        if (mSelectedList != null) mSelectedList.clear();
    }

    public void updateList() {
        mFileList.clear();
        HashMap<String, File> filemap = mExplorer.getFileMap();
        for (String s : filemap.keySet()) {
            File f = filemap.get(s);
            FileListItem i = new FileListItem(s, f.getPath(), f.isDirectory());
            if (mSelectedList.contains(f.getPath())) i.setSelected(true);
            mFileList.add(i);
        }
        // Set a ".." directory if we are not at the root.
        File currentParent = mCurrentDirectory.getParentFile();
        if (currentParent != null && currentParent.exists()) mFileList.add(0, new FileListItem("..", currentParent.getPath(), true));
        mListAdapter.notifyDataSetChanged();
    }

    public void navigateToFile(String path) {
        mCurrentDirectory = new File(path);
        mExplorer.setCurrentDirectory(mCurrentDirectory);
        mCurrentTextView.setText(path);
        updateList();
        mCurrentActivity.setClientDirString(path);
    }

    public void setItemSelected(String path, boolean selected) {
        /**
         * I'm not sure about the exact behavior of HashSet, so I'm not sure if the .contains() check is necessary.
         * I'm also assuming that if I add an item that is already in the set, nothing changes.
         */
        if (selected) {
            mSelectedList.add(path);
            mCurrentActivity.getInterface().printStatus("CLIENT:" + path + " added to selected-list.");
        } else {
            if (mSelectedList.contains(path)) {
                mSelectedList.remove(path);
                mCurrentActivity.getInterface().printStatus("CLIENT:" + path + " removed from selected-list.");
            }
        }

        updateList();
        mCurrentActivity.updateSelectedTab();
    }

    public String getCurrentDirectory() {
        return mCurrentDirectory.getPath();
    }

    public HashSet<String> getSelectedFiles() {
        if (mSelectedList != null) return mSelectedList;
        else return new HashSet<>();
    }

    public int getListItemLayout() {return R.layout.file_list_row_red;}
}
