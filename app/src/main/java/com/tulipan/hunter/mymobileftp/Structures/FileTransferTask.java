package com.tulipan.hunter.mymobileftp.Structures;

import android.os.AsyncTask;
import android.util.Log;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by Hunter on 4/27/2017.
 */
public class FileTransferTask extends AsyncTask<TransferItem, Integer, Boolean> {
    MyFTPActivity mParentActivity;
    FTPClientWrapper mClient;
    TransferItem mItem;

    private boolean PAUSED = false;

    public FileTransferTask(MyFTPActivity context, FTPClientWrapper client, TransferItem item) {
        mParentActivity = context;
        mClient = client;
        mItem = item;
    }

    public TransferItem getItem() {
        return mItem;
    }

    protected Boolean doInBackground(TransferItem ... params) {
        Boolean result = Boolean.FALSE;

        /**
         * This is currently only set up to do client to server transfers.
         */
        if (mItem.fileLocation == TransferManager.CLIENT) {
            mClient.changeWorkingDirectory(mItem.targetPath);
            String name = ensureNoServerCollision(mItem.fileName);
            try {
                OutputStream stream = mClient.storeFileStream(name);
                File file = new File(mItem.filePath);
                long length = file.length();
                long written = 0L;
                byte[] buffer = new byte[10000];
                FileInputStream reader = new FileInputStream(file);
                int numBytes = 0;
                double percent = 0.0d;
                double lastReport = 0.0d;
                while (written < length) {
                    if (PAUSED) {
                        //publishProgress(dtoi(percent), dtoi(written/1000));
                        continue;
                    }
                    numBytes = reader.read(buffer);
                    stream.write(buffer, 0, numBytes);
                    written += numBytes;
                    numBytes = 0;
                    percent = (((double)written) / length) * 100;
                    if ((percent - lastReport) > 1) {
                        publishProgress(dtoi(percent), dtoi(written/1000));
                        lastReport = percent;
                    }
                }
                reader.close();
                stream.flush();
                stream.close();
                result = Boolean.TRUE;
            } catch (Exception e) {
                Log.e(mParentActivity.TAG, "Exception in storeFile(): " + e.getMessage());
                result = Boolean.FALSE;
            } finally {
                result = (result && mClient.completePendingCommand());
            }

            // TODO: I feel like there should be a better way to get back to the home directory, but I don't know what it would be.
            mClient.returnToHome();
            /*
            while(!mClient.printWorkingDirectory().equals("/")) {
                 mClient.changeToParentDirectory();
            }
            */

        } else if (mItem.fileLocation == TransferManager.SERVER) {
            String target;
            if (mItem.targetPath.equals("/")) target = "/" + mItem.fileName;
            else target = mItem.targetPath + "/" + mItem.fileName;
            target = ensureNoClientCollision(target);
            try {
                InputStream stream = mClient.retrieveFileStream(mItem.filePath);
                FTPFile file = mClient.getFileInfo(mItem.filePath);
                long length = file.getSize();
                long written = 0L;
                byte[] buffer = new byte[10000];
                File newFile = new File(target);
                FileOutputStream writer = new FileOutputStream(newFile);
                int numBytes = 0;
                double percent = 0.0d;
                double lastReport = 0.0d;
                while (written < length) {
                    if (PAUSED) {
                        continue;
                    }
                    numBytes = stream.read(buffer);
                    writer.write(buffer, 0, numBytes);
                    written += numBytes;
                    numBytes = 0;
                    percent = (((double)written) / length) * 100;
                    if ((percent - lastReport) > 1) {
                        publishProgress(dtoi(percent), dtoi(written/1000));
                        lastReport = percent;
                    }
                }
                stream.close();
                writer.flush();
                writer.close();
                result = Boolean.TRUE;
            } catch (Exception e) {
                Log.e(mParentActivity.TAG, "Exception in storeFile(): " + e.getMessage());
                result = Boolean.FALSE;
            } finally {
                result = (result && mClient.completePendingCommand());
            }
        }

        return result;
    }

    protected void onPreExecute() {
        mParentActivity.getInterface().printStatus((mItem.fileLocation == TransferManager.CLIENT ? "CLIENT:" : "SERVER:") + mItem.filePath + " transfer initiated.");
    }

    protected void onProgressUpdate(Integer ... progress) {
        mParentActivity.postTransferProgress(progress[0], progress[1]);
    }

    protected void onPostExecute(Boolean result) {
        if (result) {
            mParentActivity.getInterface().printStatus((mItem.fileLocation == TransferManager.CLIENT ? "CLIENT:" : "SERVER:") + mItem.filePath+ " successfully transferred.");
            mParentActivity.initiateNextTransfer(4);
        }
        else {
            mParentActivity.getInterface().printError((mItem.fileLocation == TransferManager.CLIENT ? "CLIENT:" : "SERVER:") + mItem.filePath + " transfer unsuccessful.");
            mParentActivity.initiateNextTransfer(3);
        }
    }

    public void setPaused(boolean paused) {
        PAUSED = paused;
    }

    private int dtoi(double d) {
        int t = (int)d;

        if (d - t < 0.00001) return t;
        else return t+1;
    }

    private String ensureNoServerCollision(String filename) {
        String name, extension;
        String newName = filename;
        if (filename.contains(".")) {
            name = filename.substring(0, filename.lastIndexOf('.'));
            extension = filename.substring(filename.lastIndexOf('.', filename.length()));
        } else {
            name = filename;
            extension = "";
        }
        FTPFile test = mClient.getFileInfo(filename);
        int num = 1;
        while(test != null && (test.isFile() || test.isDirectory())) {
            newName = name + "(" + (num++) + ")" + extension;
            test = mClient.getFileInfo(newName);
        }
        return newName;
    }

    private String ensureNoClientCollision(String filename) {
        String name, extension;
        String newName = filename;
        if (filename.contains(".")) {
            name = filename.substring(0, filename.lastIndexOf('.'));
            extension = filename.substring(filename.lastIndexOf('.', filename.length()));
        } else {
            name = filename;
            extension = "";
        }
        File test = new File(filename);
        int num = 1;
        while(test.exists() && (test.isFile() || test.isDirectory())) {
            newName = name + "(" + (num++) + ")" + extension;
            test = new File(newName);
        }
        return newName;
    }
}
