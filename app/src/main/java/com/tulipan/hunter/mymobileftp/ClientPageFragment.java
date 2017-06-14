package com.tulipan.hunter.mymobileftp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.Structures.FTPClientWrapper;
import com.tulipan.hunter.mymobileftp.Structures.FileExplorer;
import com.tulipan.hunter.mymobileftp.Structures.FileListAdapter;
import com.tulipan.hunter.mymobileftp.Structures.FileListItem;
import com.tulipan.hunter.mymobileftp.Structures.FileSelector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * ClientPageFragment is the Fragment that manages file exploration of the client side of the
 * connection (that is to say, the user's device). It also maintains a list of files that
 * have been selected by the user for possible transfer to the server. It is always available
 * to the user, regardless of connection status.
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

    /**
     * Initializes the fragment by setting the current working directory to be the home directory,
     * which is also the root directory.
     */
    private void initFragment() {
        if (mCurrentDirectory == null) {
            mCurrentDirectory = new File(mHomeDirectory.getPath());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.client_page_fragment, container, false);

        initInterface(v);
        updateList();

        return v;
    }

    /**
     * Initializes the Fragment's interface, this includes the list of files in the current working
     * directory, displayed in a ListView, a TextView displaying the current directory's file path,
     * the Explorer, which does the work of retrieving the file lists from the device's file system,
     * and the list of files selected by the user for transfer.
     * @param view This View will always be the primary layout for the ClientPageFragment.
     */
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

        if (mListAdapter == null) mListAdapter = new FileListAdapter(mCurrentActivity,
                R.layout.file_list_row_red, mFileList, ClientPageFragment.this);
        mListView.setAdapter(mListAdapter);
        mListView.setTextFilterEnabled(true);
    }

    /**
     * Resets the Fragment by navigating it back to the home directory and clearing the list of
     * selected files.
     */
    public void reset() {
        if (mCurrentActivity != null) navigateToFile(mHomeDirectory.getPath());
        if (mSelectedList != null) mSelectedList.clear();
    }

    /**
     * Updates the list of files displayed to the user, usually to reflect a change in the current
     * working directory.
     */
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
        if (currentParent != null && currentParent.exists()) mFileList.add(0,
                new FileListItem("..", currentParent.getPath(), true));
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Sets the Fragment's current working directory to the file corresponding to the given
     * file path, signals the explorer to retrieve that directory's file list, and then signals
     * the interface to update itself to reflect this information.
     * @param path This String the path of the file that the user has chosen to navigate to.
     */
    public void navigateToFile(String path) {
        mCurrentDirectory = new File(path);
        mExplorer.setCurrentDirectory(mCurrentDirectory);
        mCurrentTextView.setText(path);
        updateList();
        mCurrentActivity.setClientDirString(path);
    }

    /**
     * Depending on its use, this either adds a file to the list of files selected for transfer,
     * or removes a file from that list. It then signals for the interface to update itself to
     * reflect this new information.
     * @param path This String contains path of the file that is being added or removed.
     * @param selected This boolean indicates whether to file is to be added or removed from the
     *                 selected list.
     */
    public void setItemSelected(String path, boolean selected) {
        if (selected) {
            mSelectedList.add(path);
            mCurrentActivity.getInterface().printStatus("CLIENT:" + path +
                    " added to selected-list.");
        } else {
            if (mSelectedList.contains(path)) {
                mSelectedList.remove(path);
                mCurrentActivity.getInterface().printStatus("CLIENT:" + path +
                        " removed from selected-list.");
            }
        }

        updateList();
        mCurrentActivity.updateSelectedTab();
    }

    /**
     * Gets this Fragment's current working directory.
     * @return The filepath of the current working directory.
     */
    public String getCurrentDirectory() {
        return mCurrentDirectory.getPath();
    }

    /**
     * Gets the list of files selected by the user for transfer in the form of a Set.
     * @return This HashSet contains the paths of the files chosen for tranfer.
     */
    public HashSet<String> getSelectedFiles() {
        if (mSelectedList != null) return mSelectedList;
        else return new HashSet<>();
    }

    /**
     * Gets the layout for the individual list item corresponding to a file selected for transfer
     * to the server (i.e. uploaded). These items can be displayed in the same list as items selected for transfer
     * from the server to the client, but the two cases have slightly different layouts.
     * @return The XML layout file for a client-to-server file tranfer.
     */
    public int getListItemLayout() {return R.layout.file_list_row_red;}
}
