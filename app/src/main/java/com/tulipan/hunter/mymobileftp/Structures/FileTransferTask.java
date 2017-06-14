package com.tulipan.hunter.mymobileftp.Structures;

import android.os.AsyncTask;
import android.util.Log;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * FileTransferTask is an extension of AsyncTask which performs the file transfers between
 * Client and Server asynchronous from the the operations of the UI. It carries references to the
 * app's Activity, the Activity's Transfer Manager, and the FTPSClient (wrapped for error handling).
 * Each instance of FileTransferTask performs a single file transfer based on the information
 * contained in the TransferItem it is constructed with. It performs the transfer using Input- and
 * Output-Streams in chunks of 10kB. It does this so that progress can be reported to the UI
 * regularly for the user to stay updated. It also allows the transfer to be paused and resumed
 * without the Task being canceled and restarted.
 */
public class FileTransferTask extends AsyncTask<TransferItem, Integer, Boolean> {
    MyFTPActivity mParentActivity;
    TransferManager mManager;
    FTPClientWrapper mClient;
    TransferItem mItem;

    private boolean PAUSED = false;

    public FileTransferTask(MyFTPActivity context, TransferManager manager,
                            FTPClientWrapper client, TransferItem item) {
        mParentActivity = context;
        mManager = manager;
        mClient = client;
        mItem = item;
    }

    public TransferItem getItem() {
        return mItem;
    }

    /**
     * Performs the work of transferring a file to or from the Server. Reads and writes in 10kB
     * chunks and keeps track of file length and the the number of bytes written in order to
     * accurately give the UI progress updates via publishProgress().
     * @param params The way AsyncTask is designed, this function could take any number of
     *               TransferItems as parameters, but this implementation does not use this
     *               functionality. Instead each Task is paired with only one TransferItem, which
     *               is passed to the Task via the Constructor.
     * @return This boolean indicates the success or failure of the transfer.
     */
    protected Boolean doInBackground(TransferItem ... params) {
        Boolean result = Boolean.FALSE;

        if (mItem.fileLocation == TransferManager.CLIENT) {
            mClient.changeWorkingDirectory(mItem.targetPath);
            String name = ensureNoServerCollision(mItem.fileName);
            try {
                OutputStream writer = mClient.storeFileStream(name);    // Stream to destination
                File file = new File(mItem.filePath);                   // File to be transferred
                long length = file.length();
                long written = 0L;
                byte[] buffer = new byte[10000];
                FileInputStream reader = new FileInputStream(file);     // Stream from File
                int numBytes;
                double percent;
                double lastReport = 0.0d;
                while (written < length) {
                    // While the transfer is paused, the rest of the loop is skipped
                    if (PAUSED) {
                        continue;
                    }
                    numBytes = reader.read(buffer);
                    writer.write(buffer, 0, numBytes);
                    written += numBytes;
                    percent = (((double)written) / length) * 100;
                    // Only publishes progress once there has been a 1% change
                    if ((percent - lastReport) > 1) {
                        publishProgress(dtoi(percent), dtoi(written/1000));
                        lastReport = percent;
                    }
                }
                // Cleanup
                reader.close();
                writer.flush();
                writer.close();
                result = Boolean.TRUE;
            } catch (Exception e) {
                Log.e(mParentActivity.TAG, "Exception in storeFile(): " + e.getMessage());
                result = Boolean.FALSE;
            } finally {
                // The completePendingCommand() function must be called after each transfer, or
                //      erratic behavior can follow.
                result = (result && mClient.completePendingCommand());
            }

            mClient.returnToHome();

        } else if (mItem.fileLocation == TransferManager.SERVER) {
            String target;
            if (mItem.targetPath.equals("/")) target = "/" + mItem.fileName;
            else target = mItem.targetPath + "/" + mItem.fileName;
            target = ensureNoClientCollision(target);
            try {
                // Stream from file on Server
                InputStream reader = mClient.retrieveFileStream(mItem.filePath);
                FTPFile file = mClient.getFileInfo(mItem.filePath);
                long length = file.getSize();
                long written = 0L;
                byte[] buffer = new byte[10000];
                File newFile = new File(target);
                // Stream to new file on Client
                FileOutputStream writer = new FileOutputStream(newFile);
                int numBytes;
                double percent;
                double lastReport = 0.0d;
                while (written < length) {
                    // While the transfer is paused, the rest of the loop is skipped.
                    if (PAUSED) {
                        continue;
                    }
                    numBytes = reader.read(buffer);
                    writer.write(buffer, 0, numBytes);
                    written += numBytes;
                    percent = (((double)written) / length) * 100;
                    // Only published progress once there has been a 1% change.
                    if ((percent - lastReport) > 1) {
                        publishProgress(dtoi(percent));
                        lastReport = percent;
                    }
                }
                // Cleanup
                reader.close();
                writer.flush();
                writer.close();
                result = Boolean.TRUE;
            } catch (Exception e) {
                Log.e(mParentActivity.TAG, "Exception in storeFile(): " + e.getMessage());
                result = Boolean.FALSE;
            } finally {
                // The completePendingCommand() function must be called after each transfer, or
                //      erratic behavior can follow.
                result = (result && mClient.completePendingCommand());
            }
        }

        return result;
    }

    /**
     * Sends a message to the Status Tab indicating the transfer has started.
     */
    protected void onPreExecute() {
        mParentActivity.getInterface().printStatus((mItem.fileLocation ==
                TransferManager.CLIENT ? "CLIENT:" : "SERVER:") +
                mItem.filePath + " transfer initiated.");
    }

    /**
     * Pushes a progress update to the UI thread.
     * @param progress This Integer will contain the percent of the file that has been transferred
     *                 so far.
     */
    protected void onProgressUpdate(Integer ... progress) {
        mManager.postTransferProgress(progress[0]);
    }

    /**
     * Sends a message to the Status Tab indicating the result of the completed file transfer.
     * @param result This boolean indicates whether the transfer was successful or not.
     */
    protected void onPostExecute(Boolean result) {
        if (result) {
            mParentActivity.getInterface().printStatus((mItem.fileLocation ==
                    TransferManager.CLIENT ? "CLIENT:" : "SERVER:") +
                    mItem.filePath+ " successfully transferred.");
            mManager.initiateNextTransfer(4);
        } else {
            mParentActivity.getInterface().printError((mItem.fileLocation ==
                    TransferManager.CLIENT ? "CLIENT:" : "SERVER:") +
                    mItem.filePath + " transfer unsuccessful.");
            mManager.initiateNextTransfer(3);
        }
    }

    public void setPaused(boolean paused) {
        PAUSED = paused;
    }

    /**
     * Does some simple rounding of double values to allow for better reporting of integer
     * percentages.
     * @param d This double value is to be rounded to the nearest int.
     * @return This int is result of rounding d.
     */
    private int dtoi(double d) {
        int t = (int)d;

        if (d - t < 0.00001) return t;
        else return t+1;
    }

    /**
     * Ensures the name of the new file being created on the Server will not collide with an
     * existing file name. It does this by appending number in parentheses after the name if it
     * encounters a file of the same name.
     * @param filename This String contains the initial file name.
     * @return This String contains the file name that does not collide with an existing one.
     */
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

    /**
     * Ensures the name of the new file being created on the Client will not collide with an
     * existing file name. It does this by appending number in parentheses after the name if it
     * encounters a file of the same name.
     * @param filename This String contains the initial file name.
     * @return This String contains the file name that does not collide with an existing one.
     */
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
