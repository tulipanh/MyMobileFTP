package com.tulipan.hunter.mymobileftp.Structures;

import android.util.Log;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;

import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by Hunter on 4/13/2017.
 */
public class FTPClientWrapper {
    private FTPSClient mClient;
    private MyFTPActivity mActivity;

    public FTPClientWrapper(MyFTPActivity activity, FTPSClient client) {
        mClient = client;
        mActivity = activity;
    }

    public FTPFile[] listFiles(String dirPath) {
        FTPFile[] files = null;
        try {
            files = mClient.listFiles(dirPath);
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from listFiles().");
        }
        return files;
    }

    public String printWorkingDirectory() {
        String directory = null;
        try {
            directory = mClient.printWorkingDirectory();
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from printWorkingDirectory()");
        }
        return directory;
    }

    public void changeToParentDirectory() {
        try {
            mClient.changeToParentDirectory();
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from changeToParentDirectory()");
        }
    }

    public void changeWorkingDirectory(String path) {
        try {
            mClient.changeWorkingDirectory(path);
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from changeWorkingDirectory()");
        }
    }

    public OutputStream storeFileStream(String remote) {
        OutputStream result;
        try {
            result = mClient.storeFileStream(remote);
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from storeFileStream()");
            result = null;
        }
        return result;
    }

    public InputStream retrieveFileStream(String remote) {
        InputStream result;
        try {
            result = mClient.retrieveFileStream(remote);
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from retrieveFileStream()");
            result = null;
        }
        return result;
    }

    public FTPFile getFileInfo(String remote) {
        FTPFile result;
        try {
            result = mClient.mlistFile(remote);
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from getFileInfo()");
            result = null;
        }
        return result;
    }

    public void returnToHome() {
        try {
            while (!mClient.printWorkingDirectory().equals("/")) {
                mClient.changeToParentDirectory();
            }
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from returnToHome()");
        }
    }

    public void deleteFile(String name, String dirPath) {
        try {
            mClient.changeWorkingDirectory(dirPath);
            mClient.deleteFile(name);
        } catch (IOException e) {
            Log.e(mActivity.TAG, "IOException from deleteFile()");
        }

        returnToHome();
    }

    public boolean isConnected() {
        return mClient.isConnected();
    }

    public void configure(FTPClientConfig config) {
        mClient.configure(config);
    }

    public void connect(InetAddress host, int port) {
        try {
            mClient.connect(host, port);
        } catch (SocketException se) {
            Log.e(mActivity.TAG, "SocketException from connect(): " + se.getMessage());
            mActivity.getInterface().printError("Could not connect to server.");
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from connect(): " + ioe.getMessage());
            mActivity.getInterface().printError("Could not connect to server.");
        }
    }

    public int getReplyCode() {
        return mClient.getReplyCode();
    }

    public void disconnect() {
        try {
            mClient.disconnect();
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from disconnect(): " + ioe.getMessage());
        }
    }

    public void logout() {
        try {
            mClient.logout();
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from logout(): " + ioe.getMessage());
        }
    }

    public void setBufferSize(int size) {
        mClient.setBufferSize(size);
    }

    public boolean login(String username, String password) {
        try {
            return mClient.login(username, password);
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from login(): " + ioe.getMessage());
            return false;
        }
    }

    public boolean setFileType(int type) {
        try {
            return mClient.setFileType(type);
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from setFileType(): " + ioe.getMessage());
            return false;
        }
    }

    public void enterLocalPassiveMode() {
        mClient.enterLocalPassiveMode();
    }

    public boolean sendCommand(String command) {
        try {
            mClient.sendCommand(command);
            return true;
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from sendCommand(): " + ioe.getMessage());
            return false;
        }
    }

    public boolean execPROT(String prot) {
        try {
            mClient.execPROT(prot);
            return true;
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from execPROT(): " + ioe.getMessage());
            return false;
        }
    }

    public boolean completePendingCommand() {
        try {
            return mClient.completePendingCommand();
        } catch (IOException ioe) {
            Log.e(mActivity.TAG, "IOException from completePendingCommand(): " + ioe.getMessage());
            return false;
        }
    }
}
