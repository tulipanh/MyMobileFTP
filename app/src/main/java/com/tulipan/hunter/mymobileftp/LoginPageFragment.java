package com.tulipan.hunter.mymobileftp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.apache.commons.net.PrintCommandListener;
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
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Created by Hunter on 12/1/2016.
 */

/**
 * TODO:
 * One key question with this fragment will be how to handle password input and communication
 * in a secure manner.
 */
public class LoginPageFragment extends Fragment {
    private MyFTPActivity mCurrentActivity;
    private LoginInterface mLoginInterface;
    private FTPSClient mFTPClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentActivity = (MyFTPActivity) getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.login_page_fragment, container, false);

        mFTPClient = mCurrentActivity.getClient();
        FTPClientConfig config = new FTPClientConfig();
        mFTPClient.configure(config);
        mLoginInterface = new LoginInterface(v);
        mFTPClient.addProtocolCommandListener(new StatusListener());

        return v;
    }

    private class StatusListener implements ProtocolCommandListener {

        @Override
        public void protocolReplyReceived(ProtocolCommandEvent event) {
            String message = "SERVER: " + event.getReplyCode() + " - " + event.getMessage();
            mCurrentActivity.getInterface().printStatus(message);
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
            mCurrentActivity.getInterface().printStatus(message);
        }
    }

    private class LoginInterface implements View.OnClickListener {
        private View mView;
        private LinearLayout mLayout;

        private EditText mAddressText;
        private EditText mPortText;
        private EditText mUsernameText;
        private EditText mPasswordText;

        private Button mLoginButton;
        private Button mDisconnectButton;

        public LoginInterface(View view) {
            mView = view;
            mLayout = (LinearLayout) mView.findViewById(R.id.login_page_layout);

            mAddressText = (EditText) mView.findViewById(R.id.address_edittext);
            mPortText = (EditText) mView.findViewById(R.id.port_edittext);
            mUsernameText = (EditText) mView.findViewById(R.id.username_edittext);
            mPasswordText = (EditText) mView.findViewById(R.id.password_edittext);

            mLoginButton = (Button) mView.findViewById(R.id.login_button);
            mDisconnectButton = (Button) mView.findViewById(R.id.disconnect_button);
            mLoginButton.setOnClickListener(this);
            mDisconnectButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            switch(v.getId()) {
                case R.id.login_button:
                    if(!networkAccessible()) break;
                    if(!inputsValid()) break;
                    try {
                        attemptConnect();
                        attemptLogin();
                        mCurrentActivity.getInterface().setConnectedText(true, mAddressText.getText().toString(), mUsernameText.getText().toString());
                        setConnected(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        cleanUpFTPClient();
                    }
                    break;

                case R.id.disconnect_button:
                    cleanUpFTPClient();
                    setConnected(false);
                    break;

                default:
                    break;
            }
            cleanUpFTPClient();
        }

        private void setConnected(boolean connected) {
            mLoginButton.setEnabled(!connected);
            mAddressText.setEnabled(!connected);
            mPortText.setEnabled(!connected);
            mUsernameText.setEnabled(!connected);
            mPasswordText.setEnabled(!connected);
            mDisconnectButton.setEnabled(connected);
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

        private void attemptConnect() {
            try {
                InetAddress address = InetAddress.getByName(mAddressText.getText().toString());
                mCurrentActivity.getInterface().printStatus("Attempting connection to " + mAddressText.getText().toString() + ".");
                mFTPClient.connect(address, Integer.parseInt(mPortText.getText().toString()));
                mCurrentActivity.getInterface().printStatus("Connected to " + mAddressText.getText().toString() + ".");

                int reply = mFTPClient.getReplyCode();

                if (!FTPReply.isPositiveCompletion(reply)) {
                    mFTPClient.disconnect();
                    mCurrentActivity.getInterface().printError("FTP server refused connection.");
                    cleanUpFTPClient();
                }
            } catch (IOException e) {
                mCurrentActivity.getInterface().printError("Could not connect to server.");
                e.printStackTrace();
                cleanUpFTPClient();
            }
        }

        private void attemptLogin() {
            try {
                mFTPClient.setBufferSize(1000);

                if (!mFTPClient.login(mUsernameText.getText().toString(), mPasswordText.getText().toString())) {
                    cleanUpFTPClient();
                }

                mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
                mFTPClient.enterLocalPassiveMode();
                mFTPClient.sendCommand("OPTS UTF8 ON");
            } catch (IOException e) {
                mCurrentActivity.getInterface().printError("Login attempt failed.");
                cleanUpFTPClient();
            }
        }
    }

    private void cleanUpFTPClient() {
        try {
            mFTPClient.logout();
        } catch (IOException e) {
            // Do nothing
        }
        mCurrentActivity.getInterface().printStatus("Logged out of server.");
        if (mFTPClient.isConnected()) {
            try {
                mFTPClient.disconnect();
            } catch (IOException f) {
                // do nothing
            }
        }
        mCurrentActivity.getInterface().printStatus("Disconnected from server.");
        mCurrentActivity.getInterface().setConnectedText(false, null, null);
        mLoginInterface.setConnected(false);
    }
}
