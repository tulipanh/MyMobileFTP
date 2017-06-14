package com.tulipan.hunter.mymobileftp;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.Structures.FTPClientWrapper;
import com.tulipan.hunter.mymobileftp.Structures.FileListAdapter;
import com.tulipan.hunter.mymobileftp.Structures.FileListItem;
import com.tulipan.hunter.mymobileftp.Structures.FileSelector;

import org.apache.commons.net.ftp.FTPFile;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * ServerPageFragment is the Fragment that manages file exploration of the server side of the
 * connection. It also maintains a list of files that have been selected by the user for possible
 * transfer to the client-side (device). It is not always available to the user. More specifically
 * it is not available until a connection has been established and a valid username + password
 * provided. It is also not available while a file is actively transferring, but can be accessed
 * by pausing the transfer queue. This because the transmission of directory information utilizes
 * the FTP connection's data channel, which gets monopolized during a file transfer.
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

    /**
     * Initializes the fragment by setting the current working directory to be the home directory,
     * which acts like the root directory from the perspective of the client, but is likely not
     * from the perspective of the server.
     */
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
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.server_page_fragment, container, false);

        initInterface(v);
        updateList();

        return v;
    }

    /**
     * Initializes the Fragment's interface, this includes the list of files in the current working
     * directory, displayed in a ListView, a TextView displaying the current directory's file path,
     * and the list of files selected by the user for transfer. Unlike the ClientPageFragment, the
     * managing of file exploration is handled by the FTPSClient, accessed through the main
     * Activity.
     * @param view This View will always be the primary layout for the ServerPageFragment.
     */
    private void initInterface(View view) {
        mListView = (ListView) view.findViewById(R.id.server_explorer_listview);
        if (mFileList == null) mFileList = new ArrayList<>();
        if (mSelectedList == null) mSelectedList = new HashSet<>();

        mCurrentTextView = (TextView) view.findViewById(R.id.server_working_directory);
        mCurrentTextView.setText(mCurrentDirectoryPath);


        if (mListAdapter == null) mListAdapter = new FileListAdapter(mCurrentActivity,
                R.layout.file_list_row_blue, mFileList, ServerPageFragment.this);
        mListView.setAdapter(mListAdapter);
        mListView.setTextFilterEnabled(true);
    }

    /**
     * Resets the Fragment by clearing the File and Selected Lists and returning the current
     * working directory to the home directory. Since this is only called when the app is
     * disconnecting, and exploration of the server files is not available when disconnected, the
     * interface does not need to be updated with new files, only cleared.
     */
    public void reset() {
        if (mCurrentDirectoryName != null) mCurrentDirectoryName = "";
        if (mCurrentDirectoryPath != null) mCurrentDirectoryPath = mHomeDirectory;
        if (mSelectedList != null) mSelectedList.clear();
        if (mFileList != null) mFileList.clear();
        if (mCurrentTextView != null) mCurrentTextView.setText(mCurrentDirectoryPath);
        if (mListAdapter != null) mListAdapter.notifyDataSetChanged();
    }

    /**
     * Updates the list of files displayed to the user, usually to reflect a change in the current
     * working directory. Retrieves the file list via a call to the FTPSClient.
     */
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

        // Set a ".." directory if we are not at the root.
        // I have found no way to get the path of the parent directory from the current directory,
        // so there is no path to give to the new List Item, there will just be a special case in
        // the navigateToFile() function below.
        if (!mCurrentDirectoryPath.equals(mHomeDirectory)) mFileList.add(0,
                new FileListItem("..", "..", true));
        mListAdapter.notifyDataSetChanged();
    }

    /**
     * Sets the Fragment's current working directory to the file corresponding to the given
     * file path, signal the interface to update itself to reflect this information.
     * @param path This String the path of the file that the user has chosen to navigate to.
     */
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
            mCurrentActivity.getInterface().printStatus("SERVER:" + path +
                    " added to selected-list.");
        } else {
            if (mSelectedList.contains(path)) {
                mSelectedList.remove(path);
                mCurrentActivity.getInterface().printStatus("SERVER:" + path +
                        " added to selected-list");
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
        return mCurrentDirectoryPath;
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
     * to the client (i.e. downloaded). These items can be displayed in the same list as items
     * selected for transfer from the client to the server, but the two cases have slightly
     * different layouts.
     * @return The XML layout file for a server-to-client file transfer.
     */
    public int getListItemLayout() {return R.layout.file_list_row_blue;}
}