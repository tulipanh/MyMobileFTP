package com.tulipan.hunter.mymobileftp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tulipan.hunter.mymobileftp.Structures.FTPClientWrapper;
import com.tulipan.hunter.mymobileftp.Structures.FileSelector;
import com.tulipan.hunter.mymobileftp.Structures.SelectedListItem;
import com.tulipan.hunter.mymobileftp.Structures.TransferItem;
import com.tulipan.hunter.mymobileftp.Structures.TransferManager;
import com.tulipan.hunter.mymobileftp.Views.ProgressTab;
import com.tulipan.hunter.mymobileftp.Views.SelectedTab;
import com.tulipan.hunter.mymobileftp.Views.StatusTab;

import org.apache.commons.net.ftp.FTPSClient;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * MyFTPActivity is the main (and only) activity of this application. It contains the FTPSClient
 * through which all communication with the server is channelled. It defines the behavior of a
 * Frame-Interface which is always present regardless of what the user is doing in the app. This
 * Frame-Interface surrounds and controls what is displayed in the middle of the app screen which
 * is the fragment-container. Inside this fragment container is displayed one of three fragments,
 * the Login Fragment, the Client Fragment, and the Server Fragment, instances of which are all
 * maintained by this Activity. In addition to these three fragments, there are three information
 * tabs, which are part of the Frame Interface. The Selected Tab shows the user which files he
 * has selected for potential transfer, the Status Tab shows the user a record of the actions he
 * has taken and information on the success or failure of those actions. The Progress Tab shows
 * the user which files are currently in the transfer-queue as well as information on the progress
 * of any file that is currently being transferred.
 */

public class MyFTPActivity extends FragmentActivity {
    public static final int REQUEST_WRITE_STORAGE = 100;
    public static final String TAG = "MyFTPActivity";
    private static boolean mPermissionGranted = false;

    private FTPClientWrapper mClient;

    private LoginPageFragment mLoginFragment;
    private ClientPageFragment mClientFragment;
    private ServerPageFragment mServerFragment;
    private FrameInterface mFrameInterface;

    private Fragment mCurrentFragment;

    private TransferManager mTransferManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_ftp_activity_layout);

        mLoginFragment = new LoginPageFragment();
        mClientFragment = new ClientPageFragment();
        mServerFragment = new ServerPageFragment();

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = mLoginFragment;
            fm.beginTransaction().add(R.id.fragment_container, fragment).commit();
            mCurrentFragment = mLoginFragment;
        }

        boolean permissions = checkPermissions();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        FTPSClient ftpClient = new FTPSClient("SSL");
        mClient = new FTPClientWrapper(this, ftpClient);

        mTransferManager = new TransferManager(MyFTPActivity.this, mClient);
        mFrameInterface = new FrameInterface(findViewById(R.id.main_activity_layout));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
    }

    /**
     * The back button will exit the entire application, so this is here to create a dialog box
     * making sure the user wishes to do that.
     */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        MyFTPActivity.this.finish();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    /**
     * Initiates a resetting of the FTPSClient and a logging of certain events to the Status Tab
     * when the Disconnect button is pressed.
     */
    public void cleanUpFTPClient() {
        mClient.logout();
        mFrameInterface.printStatus("Logged out of server.");
        if (mClient.isConnected()) mClient.disconnect();
        mFrameInterface.printStatus("Disconnected from server.");
        mLoginFragment.setConnected(false);
    }

    /**
     * replaceFragment(), switchFragment(), reloadCurrentFragment(), clearFragmentStack():
     *  These four functions are used to control the fragment that is currently visible to the
     *  user. These fragments remain in memory, and those that are not at the top of the fragment
     *  stack, they are not interactable except via the functions defined below. Three of these
     *  functions which are not used in this app are holdovers from a previous app where greater
     *  control was required, but I will keep them in case I decide to use them.
     */
    public void replaceFragment(Fragment newFragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, newFragment)
                .addToBackStack(null)
                .commit();
    }

    public void switchFragment(Fragment newFragment) {
        FragmentManager fm = getSupportFragmentManager();
        fm.beginTransaction().replace(R.id.fragment_container, newFragment)
                .commit();
    }

    public void reloadCurrentFragment() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentById(R.id.fragment_container);
        fm.beginTransaction().detach(current).attach(current).commit();
    }

    public void clearFragmentStack() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment home = new LoginPageFragment();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction().replace(R.id.fragment_container, home).addToBackStack(null).commit();
    }

    /**
     * The Selected Tab uses this function to access this Activity's ClientFragment.
     * @return The ClientFragment, which extends FileSelector.
     */
    public FileSelector getClientSelector() {
        return mClientFragment;
    }

    /**
     * The Selected Tab uses this function to access this Activity's ServerFragment.
     * @return The ServerFragment, which extends FileSelector.
     */
    public FileSelector getServerSelector() {
        return mServerFragment;
    }

    /**
     * The Selected Tab uses this function when the user chooses to initiate a transfer. It passes
     * the chosen item's information to the TransferManager and signals the Progress Tab to update.
     * @param item A SelectedListItem object used to hold information for an item in one of the
     *             Selected Tab's ListViews. Information from this object is used to initialize new
     *             objects in the TransferManager relating to the transfer of a file and the
     *             display of information in the Progress Tab.
     */
    public void addToTaskQueue(SelectedListItem item) {
        String target;
        int loc;
        if (item.isClient()) {
            target = getServerDirectory();
            loc = TransferManager.CLIENT;
        } else {
            target = getClientDirectory();
            loc = TransferManager.SERVER;
        }
        if (target.equals("")) target = "/";
        mTransferManager.addItem(item.getName(), item.getPath(), item.getSize(), target, loc);

        mFrameInterface.mProgressTab.updateList();
    }

    /**
     * Simply checks the state of the transfer queue, which can affect how other
     * aspects of the interface are displayed.
     * @return A boolean, true if the transfer queue is currently paused, false otherwise.
     */
    public boolean checkIfTransferPaused() {
        return mTransferManager.getPaused();
    }

    /**
     * Relays a command from the Progress Tab to the Transfer Manager to un-pause the
     * transfer queue.
     */
    public void resumeTransfer() {
        mTransferManager.unpause();
    }

    /**
     * Relays a command from the Progress Tab to the Transfer Manager to pause the transfer queue.
     */
    public void pauseTransfer() {
        mTransferManager.pause();
    }

    /**
     * Relays a signal from the Transfer Manager to the Progress Tab to update its list.
     */
    public void updateProgressList() {
        mFrameInterface.mProgressTab.updateList();
    }

    /**
     * Retrieves the queue of transfer operations from the TransferManager. This is largely used
     * by the Progress Tab to update itself.
     * @return An ArrayList of TransferItems, each of which contains information about an individual
     * file transfer operation.
     */
    public ArrayList<TransferItem> getTransferList() {
        return mTransferManager.getList();
    }

    /**
     * Signals to the proper file location (Client or Server) to remove an item from their list of
     * selected items. This gets called from the Transfer Manager once the transfer of said file
     * is complete or from the Progress Tab when a file is manually removed from queue.
     * @param item The TransferItem corresponding to that file transfer.
     */
    public void clearFromSelected(TransferItem item) {
        if (item.isClient()) {
            mClientFragment.setItemSelected(item.filePath, false);
        } else {
            mServerFragment.setItemSelected(item.filePath, false);
        }
    }

    /**
     * Signals the Transfer Manager to remove a item from its transfer queue, and signals the
     * corresponding file to be removed from the list of selected files.
     * @param item The TransferItem corresponding to that file transfer.
     */
    public void removeItem(TransferItem item) {
        mTransferManager.removeItem(item);
        clearFromSelected(item);
    }

    /**
     * Tells the Transfer Manager to clear the queue of file transfers.
     */
    public void clearTransferQueue() {
        mTransferManager.clearQueue();
    }

    /**
     * Tells the two file exploration fragments to reset themselves, including returning to the
     * home directory and clearing their lists of selected files. It them signals the Selected Tab
     * to update its Views.
     */
    public void cleanUpFragments() {
        mClientFragment.reset();
        mServerFragment.reset();
        mFrameInterface.mSelectedTab.updateList();
    }

    /**
     * Gets the current working directory of the client-side file explorer.
     * @return Returns a String containing either the current directory or an empty string if the
     * the fragment has not yet been initiated.
     */
    public String getClientDirectory() {
        String dir = mClientFragment.getCurrentDirectory();
        if (dir != null) return dir;
        else return "";
    }

    /**
     * Gets the current working directory of the server-side file explorer.
     * @return Returns a String containing either the current directory or an empty string if the
     * fragment has not yet been initialize or if the app has not extablished a connection.
     */
    public String getServerDirectory() {
        String dir = mServerFragment.getCurrentDirectory();
        if (dir != null) return dir;
        else return "";
    }

    /**
     * Signals the Selected Tab to update its Views.
     */
    public void updateSelectedTab() {
        mFrameInterface.mSelectedTab.updateList();
    }

    /**
     * Gets the list of files selected by the user in the client-side file explorer.
     * @return Returns a HashSet of Strings representing the file-paths.
     */
    public HashSet<String> getClientFiles() {
        return mClientFragment.getSelectedFiles();
    }

    /**
     * Gets the list of files selected by the user in the server-side file explorer.
     * @return Returns a HashSet of Strings representing the file-paths.
     */
    public HashSet<String> getServerFiles() {
        return mServerFragment.getSelectedFiles();
    }

    /**
     * Gets the main FTPSClient wrapped in an exception-handling wrapper. This is used by other
     * pieces of this app to gain access to said FTPSClient.
     * @return Returns this Activity's FTPClientWrapper, which contains the Apache Commons
     * FTPSClient and handles exceptions that may be thrown by its use.
     */
    public FTPClientWrapper getClient() {
        return mClient;
    }

    /**
     * Gets the Activity's frame-interface. This is used by other pieces of this app to gain access
     * to to elements of the frame-interface.
     * @return Returns this Activity's FrameInterface.
     */
    public FrameInterface getInterface() {
        return mFrameInterface;
    }

    /**
     * checkPermissions() and onRequestPermissionsResult():
     *  These two functions are called when the App is initialized in order to ensure that the
     *  system permissions required to run the app are granted by the user. If they are not, the
     *  App cannot run.
     */
    public boolean checkPermissions() {
        if (mPermissionGranted) {
            return true;
        } else {
            if (!(ContextCompat.checkSelfPermission(
                    MyFTPActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            } else {
                mPermissionGranted = true;
            }
            return mPermissionGranted;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] perms, int[] results) {
        super.onRequestPermissionsResult(requestCode, perms, results);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE:
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissionGranted = true;
                } else {
                    mFrameInterface.printError("Required write permissions were denied.");
                    mPermissionGranted = false;
                }
                break;
        }
    }

    /**
     * Tells the Frame Interface to change the color of the icon on the Login Fragment's tab
     * depending on whether or not a connection has been established.
     * @param connected Is a boolean whose value reflects the connection status, true of connected,
     *                  and false otherwise.
     */
    public void setConnected(boolean connected) {
        if (connected) {
            mFrameInterface.mLoginIcon.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_import_export_purple_48dp));
        } else {
            mFrameInterface.mLoginIcon.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_import_export_grey_48dp));
        }
    }

    /**
     * Parses the file-path of the current working directory of server-side file explorer, and
     * abbreviates it so that it will fit within the TextView of the Frame Interface tab
     * corresponding to the server explorer.
     * @param dir Is a String containing the file path of the server-side file explorer's current
     *            working directory.
     */
    public void setServerDirString(String dir) {
        String[] parts = dir.split("/");
        StringBuffer output = new StringBuffer();
        output.append('/');
        if (parts.length > 3) {
            addFileName(output, parts[1]);
            output.append('/');
            output.append(".../");
            addFileName(output, parts[parts.length - 1]);
            output.append('/');
        } else {
            for (int i = 1; i < parts.length; i++) {
                addFileName(output, parts[i]);
                output.append('/');
            }
        }
        mFrameInterface.mServerDirString.setText(output.toString());
    }

    /**
     * Parses the file-path of the current working directory of the client-side file explorer, and
     * abbreviates it so that it will fit within the TextView of the Frame Interface tab
     * corresponding to the client explorer.
     * @param dir Is a String containing the file path of the client-side file explorer's current
     *            working directory.
     */
    public void setClientDirString(String dir) {
        String[] parts = dir.split("/");
        StringBuffer output = new StringBuffer();
        output.append('/');
        if (parts.length > 3) {
            addFileName(output, parts[1]);
            output.append('/');
            output.append(".../");
            addFileName(output, parts[parts.length - 1]);
            output.append('/');
        } else {
            for (int i = 1; i < parts.length; i++) {
                addFileName(output, parts[i]);
                output.append('/');
            }
        }
        mFrameInterface.mClientDirString.setText(output.toString());
    }

    /**
     * A helper function for the above two functions. Aids in abbreviation of long file names.
     * @param buff This is the StringBuffer passed by one of the two functions above. It contains
     *             the working version of the abbreviated file-path.
     * @param s This is the String containing the file or directory name which may be abbreviated.
     */
    private void addFileName(StringBuffer buff, String s) {
        if (s.length() < 10) {
            buff.append(s);
            return;
        }

        char[] chars = s.toCharArray();
        for (int i = 0; i < 4; i++) buff.append(chars[i]);
        buff.append("..");
        for (int i = chars.length - 4; i < chars.length; i++) buff.append(chars[i]);
    }

    /**
     * Below is the inner class used to control the "Frame" interface, which is available at all
     * times and with any fragment active. This includes the
     *  Selected Tab:
     *      - Contains a list of all files currently selected for transfer on both the mobile
     *          client device and the server. This tab also contains interactive elements allowing
     *          the user to initiate the transfer of said files or to deselect said files and
     *          remove them from the list.
     *  Status Tab:
     *      - Contains an ongoing log of all messages sent to the server via Apache Commons'
     *          FTPSClient as well as all responses from the server. The purpose of this log is
     *          just to provide information to the user and is not interactive.
     *  Progress Tab:
     *      - Contains a list of all files which have been queued for transfer. This includes all
     *          transfers that have yet to be started, the transfer that is currently running
     *          (in the background), and all transfers that have been completed (successfully or
     *          unsuccessfully). This tab also provides live updates on the ongoing transfer,
     *          including the percent completion and number of kilobytes transferred. The
     *          interactivity provided for the user in this tab is limited to stopping the queue
     *          from progressing and removing a file transfer from the queue.
     *  Tab Buttons:
     *      - Buttons by which the user switches between or dismisses the above tabs in the view.
     *          Only one or zero tabs may be active at any given time, so clicking the button of a
     *          selected tab dismisses it, and clicking the button of a non-selected tab brings it
     *          to view while dismissing the tab that was previously active.
     *  Connected Text:
     *      - Provides the connection status information of the user, including the IP Address of
     *          server they are connected to, and the account name by which the server knows them.
     *  Fragment Buttons:
     *      - These are technically not Buttons, but LinearLayouts used as for switching
     *          between the Login, Client, and Server fragments as well as for displaying the
     *          current working directories of the client- and server-side file explorers.
     */
    public class FrameInterface implements View.OnClickListener {
        private View mView;
        private final LinearLayout mSelect;
        private final LinearLayout mStatus;
        private final LinearLayout mProgress;
        private final View mSelectHighlight;
        private final View mStatusHighlight;
        private final View mProgressHighlight;
        private final LinearLayout mSelectedTabLayout;
        private final RecyclerView mStatusList;
        private final ListView mProgressList;
        private final FrameLayout mFragmentContainer;
        private final FrameLayout mTabsContainer;

        private final LinearLayout mLoginButton;
        private final View mLoginHighlight;
        private final ImageView mLoginIcon;
        private final LinearLayout mClientButton;
        private final View mClientHighlight;
        private final TextView mClientDirString;
        private final LinearLayout mServerButton;
        private final View mServerHighlight;
        private final TextView mServerDirString;

        private final SelectedTab mSelectedTab;
        private final StatusTab mStatusTab;
        private final ProgressTab mProgressTab;

        public FrameInterface(View v) {
            mView = v;
            mSelect = (LinearLayout) mView.findViewById(R.id.selected_button);
            mSelectHighlight = mView.findViewById(R.id.infobar_selected_expand);
            mStatus = (LinearLayout) mView.findViewById(R.id.status_button);
            mStatusHighlight = mView.findViewById(R.id.infobar_status_expand);
            mProgress = (LinearLayout) mView.findViewById(R.id.progress_button);
            mProgressHighlight = mView.findViewById(R.id.infobar_progress_expand);
            mSelectedTabLayout = (LinearLayout) mView.findViewById(R.id.selected_tab_layout);
            mStatusList = (RecyclerView) mView.findViewById(R.id.status_listview);
            mProgressList = (ListView) mView.findViewById(R.id.progress_listview);
            mFragmentContainer = (FrameLayout) mView.findViewById(R.id.fragment_container);
            mTabsContainer = (FrameLayout) mView.findViewById(R.id.tabs_container);

            mLoginButton = (LinearLayout) mView.findViewById(R.id.test_nav_bar_login);
            mLoginHighlight = mView.findViewById(R.id.test_nav_bar_login_expand);
            mLoginIcon = (ImageView) mView.findViewById(R.id.test_nav_bar_login_icon);
            mClientButton = (LinearLayout) mView.findViewById(R.id.test_nav_bar_client);
            mClientHighlight = mView.findViewById(R.id.test_nav_bar_client_expand);
            mClientDirString = (TextView) mView.findViewById(R.id.test_nav_bar_client_directory);
            mServerButton  = (LinearLayout) mView.findViewById(R.id.test_nav_bar_server);
            mServerHighlight = mView.findViewById(R.id.test_nav_bar_server_expand);
            mServerDirString = (TextView) mView.findViewById(R.id.test_nav_bar_server_directory);

            mSelectedTab = new SelectedTab(MyFTPActivity.this, mSelect, mSelectedTabLayout);
            mStatusTab = new StatusTab(MyFTPActivity.this, mStatus, mStatusList);
            mProgressTab = new ProgressTab(MyFTPActivity.this, mProgress, mProgressList);

            mSelect.setOnClickListener(this);
            mStatus.setOnClickListener(this);
            mProgress.setOnClickListener(this);
            mTabsContainer.setOnClickListener(this);
            mLoginButton. setOnClickListener(this);
            mClientButton.setOnClickListener(this);
            mServerButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if(v.isSelected()) {
                resetInfobarElevations();
                mSelectedTab.setSelected(false);
                mStatusTab.setSelected(false);
                mProgressTab.setSelected(false);
                mTabsContainer.setVisibility(View.GONE);
            }
            else {
                switch (v.getId()) {
                    case R.id.selected_button:
                        resetInfobarElevations();
                        mSelectedTab.setSelected(true);
                        mProgressTab.setSelected(false);
                        mStatusTab.setSelected(false);
                        mTabsContainer.setVisibility(View.VISIBLE);
                        mSelect.setElevation(10);
                        mSelectHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                        break;

                    case R.id.progress_button:
                        resetInfobarElevations();
                        mProgressTab.setSelected(true);
                        mSelectedTab.setSelected(false);
                        mStatusTab.setSelected(false);
                        mTabsContainer.setVisibility(View.VISIBLE);
                        mProgress.setElevation(10);
                        mProgressHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                        break;

                    case R.id.status_button:
                        resetInfobarElevations();
                        mStatusTab.setSelected(true);
                        mProgressTab.setSelected(false);
                        mSelectedTab.setSelected(false);
                        mTabsContainer.setVisibility(View.VISIBLE);
                        mStatus.setElevation(10);
                        mStatusHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                        break;

                    case R.id.tabs_container:
                        break;

                    case R.id.test_nav_bar_login:
                        resetNavButtons();
                        mLoginButton.setElevation(10);
                        mLoginHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                        if (mCurrentFragment != mLoginFragment) {
                            switchFragment(mLoginFragment);
                            mCurrentFragment = mLoginFragment;
                        }
                        break;

                    case R.id.test_nav_bar_client:
                        resetNavButtons();
                        mClientButton.setElevation(10);
                        mClientHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                        if (mCurrentFragment != mClientFragment) {
                            switchFragment(mClientFragment);
                            mCurrentFragment = mClientFragment;
                        }
                        break;

                    case R.id.test_nav_bar_server:
                        if (mClient.isConnected()) {
                            resetNavButtons();
                            mServerButton.setElevation(10);
                            mServerHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                            if (mCurrentFragment != mServerFragment) {
                                switchFragment(mServerFragment);
                                mCurrentFragment = mServerFragment;
                            }
                        } else {
                            Toast.makeText(MyFTPActivity.this,
                                    "Must be connected and logged in to access server browsing.",
                                    Toast.LENGTH_LONG).show();
                        }
                        break;

                    default:
                        mSelectedTab.setSelected(false);
                        mProgressTab.setSelected(false);
                        mStatusTab.setSelected(false);
                        mTabsContainer.setVisibility(View.GONE);
                        break;
                }
            }
        }

        public void resetNavButtons() {
            mLoginButton.setElevation(6);
            mClientButton.setElevation(6);
            mServerButton.setElevation(6);
            mLoginHighlight.setBackgroundResource(R.color.transparent);
            mClientHighlight.setBackgroundResource(R.color.transparent);
            mServerHighlight.setBackgroundResource(R.color.transparent);
        }

        public void resetInfobarElevations() {
            mSelect.setElevation(6);
            mStatus.setElevation(6);
            mProgress.setElevation(6);
            mSelectHighlight.setBackgroundResource(R.color.transparent);
            mStatusHighlight.setBackgroundResource(R.color.transparent);
            mProgressHighlight.setBackgroundResource(R.color.transparent);
        }

        public void setServerBrowsing(boolean on) {
            if (on) {
                mServerButton.setEnabled(true);
            } else {
                mServerButton.setEnabled(false);
                // Navigate to Login Fragment if currently at Server Fragment.
                if (mCurrentFragment == mServerFragment) {
                    resetNavButtons();
                    mLoginButton.setElevation(10);
                    mLoginHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                    switchFragment(mLoginFragment);
                    mCurrentFragment = mLoginFragment;
                }
            }
        }

        public void printError(String message) {
            mStatusTab.addError(message);
        }

        public void printWarning(String message) {
            mStatusTab.addWarning(message);
        }

        public void printStatus(String message) {
            mStatusTab.addStatus(message);
        }

        public void printSentMessage(String message) {
            mStatusTab.addSentMessage(message);
        }
    }
}
