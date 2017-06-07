package com.tulipan.hunter.mymobileftp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public class MyFTPActivity extends FragmentActivity {
    public static final int REQUEST_WRITE_STORAGE = 100;
    public static final String TAG = "MyFTPActivity";
    private static boolean mPermissionGranted = false;

    private static FTPSClient mFTPClient;
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

        /**
         * Infrastructure setup.
         * TODO:
         * Probably some work to be done here regarding FTPClient setup as well as
         * some more permissions requesting regarding internet.
         */
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = mLoginFragment;
            fm.beginTransaction().add(R.id.fragment_container, fragment).commit();
            setCurrentFragment(mLoginFragment);
        }

        boolean permissions = checkPermissions();

        /**
         * TODO:
         * Probably want to run all network connections in a separate thread.
         * Even connections regarding the control port, but until I get the functionality
         * working the way I want, I will run everything on the main thread to simplify things.
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mFTPClient = new FTPSClient("SSL");
        mClient = new FTPClientWrapper(this, mFTPClient);

        mTransferManager = new TransferManager(MyFTPActivity.this, mClient);
        mFrameInterface = new FrameInterface((LinearLayout) findViewById(R.id.main_activity_layout));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        /**
         * TODO:
         * Instance state will definitely include saving login info (not passwords
         * though) and last viewed folder (client side), and it may include helpful
         * things like a basic transaction history.
         */
        super.onSaveInstanceState(savedInstanceState);
    }

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
     *  user. These fragments remain in memory, which they are not at the top of the fragment
     *  stack, they are not interactable except via the functions defined below.
     *
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

    public FileSelector getClientSelector() {
        return mClientFragment;
    }

    public FileSelector getServerSelector() {
        return mServerFragment;
    }

    public void addToTaskQueue(SelectedListItem item) {
        /**
         * TODO:
         *  From here, an item should be added to the progress tab indicating that the file transfer is in queue.
         *  Simultaneously, an item should be added to the TransferManager, actually adding it to the queue.
         */
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

    public boolean checkIfTransferPaused() {
        return mTransferManager.getPaused();
    }

    public void resumeTransfer() {
        mTransferManager.unpause();
    }

    public void pauseTransfer() {
        mTransferManager.pause();
    }

    public void postTransferProgress(Integer percent, Integer numKBytes) {
        mTransferManager.updateProgress(percent, numKBytes);
        mFrameInterface.mProgressTab.updateList();
    }

    public void initiateNextTransfer(Integer result) {
        mTransferManager.initiateNextTransfer(result);
        mFrameInterface.mProgressTab.updateList();
    }

    public ArrayList<TransferItem> getTransferList() {
        return mTransferManager.getList();
    }

    public void clearFromSelected(TransferItem item) {
        if (item.isClient()) {
            mClientFragment.setItemSelected(item.filePath, false);
        } else {
            mServerFragment.setItemSelected(item.filePath, false);
        }
    }

    public void removeItem(TransferItem item) {
        mTransferManager.removeItem(item);
        clearFromSelected(item);
        mFrameInterface.mProgressTab.updateList();
    }

    public void clearTransferQueue() {
        mTransferManager.clearQueue();
    }

    public void cleanUpFragments() {
        mClientFragment.reset();
        mServerFragment.reset();
        mFrameInterface.mSelectedTab.updateList();
    }

    public String getClientDirectory() {
        String dir = mClientFragment.getCurrentDirectory();
        if (dir != null) return dir;
        else return "";
    }

    public String getServerDirectory() {
        String dir = mServerFragment.getCurrentDirectory();
        if (dir != null) return dir;
        else return "";
    }

    public void updateSelectedTab() {
        mFrameInterface.mSelectedTab.updateList();
    }

    public HashSet<String> getClientFiles() {
        return mClientFragment.getSelectedFiles();
    }
    public HashSet<String> getServerFiles() { return mServerFragment.getSelectedFiles(); }

    public FTPClientWrapper getClient() {
        return mClient;
    }

    public FrameInterface getInterface() {
        return mFrameInterface;
    }

    public void setCurrentFragment(Fragment f) {
        mCurrentFragment = f;
    }

    /**
     * checkPermissions() and onRequestPermissionsResult():
     *  These two functions are called when the App is initialized in order to ensure that the
     *  system permissions required to run the app are granted by the user. If they are not, the
     *  App cannot run.
     */
    public boolean checkPermissions() {
        /**
         * TODO:
         * If more than one type of permission must be requested, this method and the
         * onRequestPermissionsResult method will need to be rewritten to accommodate that.
         */
        if (mPermissionGranted) {
            return true;
        } else {
            if (!(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED)) {
                ActivityCompat.requestPermissions(this, new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            } else {
                mPermissionGranted = true;
            }
            return mPermissionGranted;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mPermissionGranted = true;
                } else {
                    mFrameInterface.printError("Required write permissions were denied.");
                    // TODO: Need some way to alert the user that permissions are needed for the app to do its job.
                    mPermissionGranted = false;
                }
                break;
        }
    }

    public void setConnected(boolean connected) {
        if (connected) {
            mFrameInterface.mLoginIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_import_export_purple_48dp));
        } else {
            mFrameInterface.mLoginIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_import_export_grey_48dp));
        }
    }

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

    public boolean checkConnection() {
        return mClient.isConnected();
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
     *      - These are placeholder buttons used for easily switching between the Login, Client,
     *          and Server fragments during development and testing. These will likely be replaced
     *          by some kind of swiping interaction or possibly a radial menu.
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
                        if (checkConnection()) {
                            resetNavButtons();
                            mServerButton.setElevation(10);
                            mServerHighlight.setBackgroundResource(R.drawable.nav_bar_highlight);
                            if (mCurrentFragment != mServerFragment) {
                                switchFragment(mServerFragment);
                                mCurrentFragment = mServerFragment;
                            }
                        } else {
                            Toast.makeText(MyFTPActivity.this, "Must be connected and logged in to access server browsing.", Toast.LENGTH_LONG).show();
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
