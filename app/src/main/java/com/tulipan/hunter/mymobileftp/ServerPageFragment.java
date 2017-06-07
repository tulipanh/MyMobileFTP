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
import com.tulipan.hunter.mymobileftp.Structures.FileListAdapter;
import com.tulipan.hunter.mymobileftp.Structures.FileListItem;
import com.tulipan.hunter.mymobileftp.Structures.FileSelector;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Hunter on 4/12/2017.
 */
public class ServerPageFragment extends Fragment implements FileSelector {
    private MyFTPActivity mCurrentActivity;
    private FTPClientWrapper mClient = null;
    private String mCurrentDirectoryPath = null;
    private String mCurrentDirectoryName = null;
    private TextView mCurrentTextView = null;

    private String mHomeDirectory;
    private HashSet<String> mSelectedList = null;

    private ListView mListView = null;
    private FileListAdapter mListAdapter = null;
    private ArrayList<FileListItem> mFileList = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = (MyFTPActivity) getActivity();
        mClient = mCurrentActivity.getClient();

        initFragment();
    }

    private void initFragment() {
        mHomeDirectory = mClient.printWorkingDirectory() ;
        if (mCurrentDirectoryPath == null) {
            mCurrentDirectoryPath = mHomeDirectory;
        }
        if (mCurrentDirectoryName == null) {
            mCurrentDirectoryName = "";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.server_page_fragment, container, false);

        initInterface(v);
        updateList();

        return v;
    }

    private void initInterface(View view) {
        mListView = (ListView) view.findViewById(R.id.server_explorer_listview);
        if (mFileList == null) mFileList = new ArrayList<>();
        if (mSelectedList == null) mSelectedList = new HashSet<>();

        mCurrentTextView = (TextView) view.findViewById(R.id.server_working_directory);
        mCurrentTextView.setText(mCurrentDirectoryPath);


        if (mListAdapter == null) mListAdapter = new FileListAdapter(mCurrentActivity, R.layout.file_list_row_blue, mFileList, ServerPageFragment.this);
        mListView.setAdapter(mListAdapter);
        mListView.setTextFilterEnabled(true);
    }

    public void reset() {
        if (mCurrentDirectoryName != null) mCurrentDirectoryName = "";
        if (mCurrentDirectoryPath != null) mCurrentDirectoryPath = mHomeDirectory;
        if (mSelectedList != null) mSelectedList.clear();
        if (mFileList != null) mFileList.clear();
        if (mCurrentTextView != null) mCurrentTextView.setText(mCurrentDirectoryPath);
        if (mListAdapter != null) mListAdapter.notifyDataSetChanged();
    }

    public void updateList() {
        mFileList.clear();
        FTPFile[] files = mClient.listFiles(mCurrentDirectoryPath);
        if (mCurrentDirectoryPath.equals(mHomeDirectory)) {
            for (FTPFile f : files) {
                String path = mCurrentDirectoryPath + f.getName();
                FileListItem i = new FileListItem(f.getName(), "/" + f.getName(), f.isDirectory());
                if (mSelectedList.contains(path)) i.setSelected(true);
                mFileList.add(i);
            }
        } else {
            for (FTPFile f : files) {
                String path = mCurrentDirectoryPath + "/" + f.getName();
                FileListItem i = new FileListItem(f.getName(), path, f.isDirectory());
                if (mSelectedList.contains(path)) i.setSelected(true);
                mFileList.add(i);
            }
        }

        // TODO: The below will have to come from some interaction with the FTPClient.
        /**
         * Need to look more into how navigation works with FTP client to see how I can tell
         * the current directory and home directory and whether or not I can see the parent
         * (i.e. if a "home" directory is set and I am denied access to anything higher up
         * in the server's file system)
         */
        // Set a ".." directory if we are not at the root.
        // This may not be necessary if the file is contained in the list given by listFiles()
        if (!mCurrentDirectoryPath.equals(mHomeDirectory)) mFileList.add(0, new FileListItem("..", "..", true));//TODO: Can't find a good way to get the name/ raw listing of the parent directory of the current working directory.
        mListAdapter.notifyDataSetChanged();
    }

    public void navigateToFile(String path) {
        if (path == "..") {
            String[] pathbits = mCurrentDirectoryPath.split("/");
            mCurrentDirectoryName = pathbits[pathbits.length-2];
            StringBuffer temp = new StringBuffer();
            temp.append('/');
            for (int i = 1; i < pathbits.length-2; i++) {
                temp.append(pathbits[i]);
                temp.append('/');
            }
            temp.append(pathbits[pathbits.length - 2]);
            mCurrentDirectoryPath = temp.toString();
        } else {
            mCurrentDirectoryPath = path;
            String[] temp = path.split("/");
            mCurrentDirectoryName = temp[temp.length-1];
        }
        mCurrentTextView.setText(mCurrentDirectoryPath);
        mCurrentActivity.setServerDirString(mCurrentDirectoryPath);
        updateList();
    }

    public void setItemSelected(String path, boolean selected) {
        /**
         * I'm not sure about the exact behavior of HashSet, so I'm not sure if the .contains() check is necessary.
         * I'm also assuming that if I add an item that is already in the set, nothing changes.
         */
        if (selected) {
            mSelectedList.add(path);
            mCurrentActivity.getInterface().printStatus("Selected SERVER:" + path + " for transfer.");
        } else {
            if (mSelectedList.contains(path)) {
                mSelectedList.remove(path);
                mCurrentActivity.getInterface().printStatus("Unselected SERVER:" + path + " for transfer.");
            }
        }

        updateList();
        mCurrentActivity.updateSelectedTab();
    }

    public String getCurrentDirectory() {
        return mCurrentDirectoryPath;
    }

    public HashSet<String> getSelectedFiles() {
        if (mSelectedList != null) return mSelectedList;
        else return new HashSet<>();
    }

    public int getListItemLayout() {return R.layout.file_list_row_blue;}
}