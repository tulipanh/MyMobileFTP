package com.tulipan.hunter.mymobileftp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.tulipan.hunter.mymobileftp.Structures.FTPClientWrapper;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.commons.validator.routines.IntegerValidator;
import org.apache.commons.validator.routines.UrlValidator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by Hunter on 12/1/2016.
 */

/**
 * TODO:
 * One key question with this fragment will be how to handle password input and communication
 * in a secure manner.
 */
public class LoginPageFragment extends Fragment implements View.OnClickListener, View.OnFocusChangeListener, CompoundButton.OnCheckedChangeListener {
    private MyFTPActivity mCurrentActivity;
    private FTPClientWrapper mClient = null;
    private SharedPreferences mPrefs;

    private LinearLayout mAddressTile = null;
    private LinearLayout mPortTile = null;
    private LinearLayout mUsernameTile = null;
    private LinearLayout mPasswordTile = null;

    private EditText mAddressText = null;
    private EditText mPortText = null;
    private EditText mUsernameText = null;
    private EditText mPasswordText = null;

    private CheckBox mAddressCheck = null;
    private CheckBox mPortCheck = null;
    private CheckBox mUsernameCheck = null;
    private CheckBox mPasswordCheck = null;
    private Button mLoginButton = null;
    private Button mDisconnectButton = null;

    // REMOVE private StatusListener mStatusListener = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = (MyFTPActivity) getActivity();
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mCurrentActivity);

        if (mClient == null) {
            mClient = mCurrentActivity.getClient();

            FTPClientConfig config = new FTPClientConfig();
            mClient.configure(config);
        }

        /**
         * I am leaving the StatusListener code in the file, but commented out so that  I can refer
         * back to it in the future as an example of how to set up a ProtocolCommandListener.
         */
        /*
        if (mStatusListener == null) {
            mStatusListener = new StatusListener();
            mFTPClient.addProtocolCommandListener(mStatusListener);
        }
        */
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.login_page_fragment, container, false);

        initInterface(v);

        return v;
    }

    @Override
    public void onClick(View v) {
        View parent;

        switch(v.getId()) {
            case R.id.login_button:
                saveCheckedInfo();
                if(!networkAccessible()) break;
                if(!inputsValid()) break;
                try {
                    if (connectAndLogin()) {
                        setConnected(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    mCurrentActivity.cleanUpFTPClient();
                }
                break;

            case R.id.disconnect_button:
                clearTransferQueue();
                mCurrentActivity.cleanUpFTPClient();
                setConnected(false);
                mCurrentActivity.cleanUpFragments();
                break;

            case R.id.login_host_tile:
                mAddressText.requestFocus();
                break;

            case R.id.login_port_tile:
                mPortText.requestFocus();
                break;

            case R.id.login_username_tile:
                mUsernameText.requestFocus();
                break;

            case R.id.login_password_tile:
                mPasswordText.requestFocus();
                break;

            default:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SharedPreferences.Editor editor = mPrefs.edit();
        switch(buttonView.getId()) {
            case R.id.login_remember_host:
                if (isChecked) editor.putBoolean("RememberHost", true);
                else {
                    editor.putBoolean("RememberHost", false);
                    editor.remove("HostValue");
                }
                editor.apply();
                break;

            case R.id.login_remember_port:
                if (isChecked) editor.putBoolean("RememberPort", true);
                else {
                    editor.putBoolean("RememberPort", false);
                    editor.remove("PortValue");
                }
                editor.apply();
                break;

            case R.id.login_remember_username:
                if (isChecked) editor.putBoolean("RememberUsername", true);
                else {
                    editor.putBoolean("RememberUsername", false);
                    editor.remove("UsernameValue");
                }
                editor.apply();
                break;

            case R.id.login_remember_password:
                if (isChecked) editor.putBoolean("RememberPassword", true);
                else {
                    editor.putBoolean("RememberPassword", false);
                    editor.remove("PasswordValue");
                }
                editor.apply();
                break;

            default:
                break;
        }
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
            selectTile((LinearLayout)v.getParent());
        }
    }

    private void selectTile(LinearLayout tile) {
        resetLoginItemElevations();
        if (tile != null) tile.setElevation(16);
    }

    private void resetLoginItemElevations() {
        mAddressTile.setElevation(0);
        mPortTile.setElevation(0);
        mUsernameTile.setElevation(0);
        mPasswordTile.setElevation(0);
    }

    private void saveCheckedInfo() {
        SharedPreferences.Editor editor = mPrefs.edit();
        if (mAddressCheck.isChecked()) editor.putString("HostValue", mAddressText.getText().toString());
        else editor.remove("HostValue");
        if (mPortCheck.isChecked()) editor.putString("PortValue", mPortText.getText().toString());
        else editor.remove("PortValue");
        if (mUsernameCheck.isChecked()) editor.putString("UsernameValue", mUsernameText.getText().toString());
        else editor.remove("UsernameValue");
        if (mPasswordCheck.isChecked()) editor.putString("PasswordValue", mPasswordText.getText().toString());
        else editor.remove("PasswordValue");
        editor.apply();
    }

    /**
     * The below class was removed because setting a ProtocolCommandListener on the FTPClient
     * was not working for when the FTPClient does work asynchronously. The Android OS has
     * prohibitions against any thread besides the UI thread from causing changes to the UI.
     * This might be circumvented somehow if the asynchronous work is done from a HandlerThread
     * of some kind rather than through AsyncTasks, but I have yet to investigate that route and
     * have no plans to do so in this project.
     */

    /*
    private class StatusListener implements ProtocolCommandListener {

        @Override
        public void protocolReplyReceived(ProtocolCommandEvent event) {
            String message = "SERVER: " + event.getReplyCode() + " - " + event.getMessage();
            if (event.getReplyCode() >= 400) mCurrentActivity.getInterface().printError(message);
            else if (event.getReplyCode() >= 300) mCurrentActivity.getInterface().printWarning(message);
            else ;//mCurrentActivity.getInterface().printStatus(message);
        }

        @Override
        public void protocolCommandSent(ProtocolCommandEvent event) {
            String message;
            String command = event.getCommand();
            if (command.equals("PASS")) {
                message = "CLIENT: " + event.getCommand() + " - " + "********";
            } else {
                message = "CLIENT: " + event.getCommand() + " - " + event.getMessage();
            }
            //mCurrentActivity.getInterface().printSentMessage(message);
        }
    }
    */

    private void initInterface(View view) {
        mAddressTile = (LinearLayout) view.findViewById(R.id.login_host_tile);
        mAddressTile.setOnClickListener(this);
        mPortTile = (LinearLayout) view.findViewById(R.id.login_port_tile);
        mPortTile.setOnClickListener(this);
        mUsernameTile = (LinearLayout) view.findViewById(R.id.login_username_tile);
        mUsernameTile.setOnClickListener(this);
        mPasswordTile = (LinearLayout) view.findViewById(R.id.login_password_tile);
        mPasswordTile.setOnClickListener(this);

        mAddressText = (EditText) view.findViewById(R.id.address_edittext);
        mAddressText.setOnFocusChangeListener(this);
        mAddressText.setText(mPrefs.getString("HostValue", ""));
        mPortText = (EditText) view.findViewById(R.id.port_edittext);
        mPortText.setOnFocusChangeListener(this);
        mPortText.setText(mPrefs.getString("PortValue", ""));
        mUsernameText = (EditText) view.findViewById(R.id.username_edittext);
        mUsernameText.setOnFocusChangeListener(this);
        mUsernameText.setText(mPrefs.getString("UsernameValue", ""));
        mPasswordText = (EditText) view.findViewById(R.id.password_edittext);
        mPasswordText.setOnFocusChangeListener(this);
        mPasswordText.setText(mPrefs.getString("PasswordValue", ""));

        mAddressCheck = (CheckBox) view.findViewById(R.id.login_remember_host);
        mAddressCheck.setChecked(mPrefs.getBoolean("RememberHost", false));
        mAddressCheck.setOnCheckedChangeListener(this);
        mPortCheck = (CheckBox) view.findViewById(R.id.login_remember_port);
        mPortCheck.setChecked(mPrefs.getBoolean("RememberPort", false));
        mPortCheck.setOnCheckedChangeListener(this);
        mUsernameCheck = (CheckBox) view.findViewById(R.id.login_remember_username);
        mUsernameCheck.setChecked(mPrefs.getBoolean("RememberUsername", false));
        mUsernameCheck.setOnCheckedChangeListener(this);
        mPasswordCheck = (CheckBox) view.findViewById(R.id.login_remember_password);
        mPasswordCheck.setChecked(mPrefs.getBoolean("RememberPassword", false));
        mPasswordCheck.setOnCheckedChangeListener(this);

        mLoginButton = (Button) view.findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(this);
        mDisconnectButton = (Button) view.findViewById(R.id.disconnect_button);
        mDisconnectButton.setOnClickListener(this);

        setConnected(mClient.isConnected());
    }

    public void setConnected(boolean connected) {
        mLoginButton.setEnabled(!connected);
        mAddressText.setEnabled(!connected);
        mPortText.setEnabled(!connected);
        mUsernameText.setEnabled(!connected);
        mPasswordText.setEnabled(!connected);
        mDisconnectButton.setEnabled(connected);
        mCurrentActivity.setConnected(connected);
    }

    private boolean networkAccessible() {
        ConnectivityManager connManager = (ConnectivityManager)
                mCurrentActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = connManager.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            mCurrentActivity.getInterface().printStatus("Network Available.");
            return true;
        } else {
            mCurrentActivity.getInterface().printError("Not connected to network.");
            return false;
        }
    }

    private boolean inputsValid() {
        boolean error = false;
        UrlValidator urlValidator = UrlValidator.getInstance();
        InetAddressValidator ipValidator = InetAddressValidator.getInstance();
        IntegerValidator intValidator = IntegerValidator.getInstance();

        // Check if address is valid.
        // URLs will only work if an application protocol is provided.
        if(!(urlValidator.isValid("ftp://" + mAddressText.getText().toString()) || ipValidator.isValid(mAddressText.getText().toString()))) {
            mCurrentActivity.getInterface().printError("Address not valid.");
            error = true;
        }
        // Check if port number is valid.
        String portString = mPortText.getText().toString();
        if((!(intValidator.isValid(portString) && Integer.parseInt(portString) >= 0 && Integer.parseInt(portString) <= 65535))) {
            mCurrentActivity.getInterface().printError("Port not valid.");
            error = true;
        }

        return !error;
    }

    private boolean connectAndLogin() {
        if (attemptConnect() && attemptLogin()) return true;
        else return false;
    }

    private boolean attemptConnect() {
        try {
            InetAddress address = InetAddress.getByName(mAddressText.getText().toString());
            mCurrentActivity.getInterface().printStatus("Attempting connection to " + mAddressText.getText().toString() + ".");
            mClient.connect(address, Integer.parseInt(mPortText.getText().toString()));
            mCurrentActivity.getInterface().printStatus("Connected to " + mAddressText.getText().toString() + ".");

            int reply = mClient.getReplyCode();

            if (!FTPReply.isPositiveCompletion(reply)) {
                mClient.disconnect();
                mCurrentActivity.getInterface().printError("FTP server refused connection.");
                mCurrentActivity.cleanUpFTPClient();
                return false;
            }

            return true;
        } catch (UnknownHostException uhe) {
            Log.e(mCurrentActivity.TAG, "UnknownHostException from attemptConnect(): " + uhe.getMessage());
            return false;
        }
    }

    private boolean attemptLogin() {
        boolean result = true;
        mClient.setBufferSize(1000);

        if (!mClient.login(mUsernameText.getText().toString(), mPasswordText.getText().toString())) {
            mCurrentActivity.cleanUpFTPClient();
            return false;
        }

        result = (result && mClient.setFileType(FTP.BINARY_FILE_TYPE));
        mClient.enterLocalPassiveMode();
        result = (result && mClient.sendCommand("OPTS UTF8 ON"));
        result = (result && mClient.execPROT("P"));

        if (!result) {
            mCurrentActivity.getInterface().printError("Login attempt failed.");
            mCurrentActivity.cleanUpFTPClient();
            return false;
        } else {
            return true;
        }
    }

    private void clearTransferQueue() {
        mCurrentActivity.clearTransferQueue();
    }
}
