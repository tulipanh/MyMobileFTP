package com.tulipan.hunter.mymobileftp;

import android.Manifest;
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
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.tulipan.hunter.mymobileftp.Views.ProgressTab;
import com.tulipan.hunter.mymobileftp.Views.SelectedTab;
import com.tulipan.hunter.mymobileftp.Views.StatusTab;

import org.apache.commons.net.ftp.FTPSClient;

import java.util.ArrayList;

public class MyFTPActivity extends FragmentActivity {
    public static final int REQUEST_WRITE_STORAGE = 100;
    public static final String TAG = "MyFTPActivity";
    private static boolean mPermissionGranted = false;

    private static FTPSClient mFTPClient;

    private FrameInterface mFrameInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_ftp_activity_layout);

        /**
         * Infrastructure setup.
         * TODO:
         * Probably some work to be done here regarding FTPClient setup as well as
         * some more permissions requesting regarding internet.
         */
        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(R.id.fragment_container);

        if (fragment == null) {
            fragment = new LoginPageFragment();
            fm.beginTransaction().add(R.id.fragment_container, fragment).commit();
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

    public void replaceFragment(Fragment newFragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, newFragment)
                .addToBackStack(null)
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

    public FTPSClient getClient() {
        return mFTPClient;
    }

    public FrameInterface getInterface() {
        return mFrameInterface;
    }

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
                    mPermissionGranted = false;
                }
                break;
        }
    }

    public class FrameInterface implements View.OnClickListener {
        private View mView;
        private final Button mSelect;
        private final Button mStatus;
        private final Button mProgress;
        private final ListView mSelectList;
        private final RecyclerView mStatusList;
        private final ListView mProgressList;
        private final FrameLayout mFragmentContainer;
        private final TextView mConnectedTextView;

        private final SelectedTab mSelectedTab;
        private final StatusTab mStatusTab;
        private final ProgressTab mProgressTab;

        public FrameInterface(View v) {
            mView = v;
            mSelect = (Button) mView.findViewById(R.id.selected_button);
            mStatus = (Button) mView.findViewById(R.id.status_button);
            mProgress = (Button) mView.findViewById(R.id.progress_button);
            mSelectList = (ListView) mView.findViewById(R.id.selected_listview);
            mStatusList = (RecyclerView) mView.findViewById(R.id.status_listview);
            mProgressList = (ListView) mView.findViewById(R.id.progress_listview);
            mFragmentContainer = (FrameLayout) mView.findViewById(R.id.fragment_container);
            mConnectedTextView = (TextView) mView.findViewById(R.id.connected_status_textview);

            mSelectedTab = new SelectedTab(MyFTPActivity.this, mSelect, mSelectList);
            mStatusTab = new StatusTab(MyFTPActivity.this, mStatus, mStatusList);
            mProgressTab = new ProgressTab(MyFTPActivity.this, mProgress, mProgressList);

            mSelect.setOnClickListener(this);
            mStatus.setOnClickListener(this);
            mProgress.setOnClickListener(this);

            mConnectedTextView.setText(R.string.not_connected);
        }

        @Override
        public void onClick(View v) {
            if(v.isSelected()) {
                mSelectedTab.setSelected(false);
                mStatusTab.setSelected(false);
                mProgressTab.setSelected(false);
                mFragmentContainer.setVisibility(View.VISIBLE);
            }
            else {
                switch (v.getId()) {
                    case R.id.selected_button:
                        mSelectedTab.setSelected(true);
                        mProgressTab.setSelected(false);
                        mStatusTab.setSelected(false);
                        mFragmentContainer.setVisibility(View.GONE);
                        break;

                    case R.id.progress_button:
                        mProgressTab.setSelected(true);
                        mSelectedTab.setSelected(false);
                        mStatusTab.setSelected(false);
                        mFragmentContainer.setVisibility(View.GONE);
                        break;

                    case R.id.status_button:
                        mStatusTab.setSelected(true);
                        mProgressTab.setSelected(false);
                        mSelectedTab.setSelected(false);
                        mFragmentContainer.setVisibility(View.GONE);
                        break;

                    default:
                        mSelectedTab.setSelected(false);
                        mProgressTab.setSelected(false);
                        mStatusTab.setSelected(false);
                        mFragmentContainer.setVisibility(View.VISIBLE);
                        break;
                }
            }
        }

        public void setConnectedText(boolean isConnected, String servername, String username) {
            if (isConnected) {
                mConnectedTextView.setText("Connected To: " + servername + "\nAs User: " + username);
            } else {
                mConnectedTextView.setText("Not Connected");
            }
        }

        public void printError(String message) {
            mStatusTab.addError(message);
        }

        public void printStatus(String message) {
            mStatusTab.addStatus(message);
        }
    }
}
