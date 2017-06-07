package com.tulipan.hunter.mymobileftp.Structures;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by Hunter on 4/26/2017.
 */

/**
 * TODO:
 *  The TransferManager should maintain a queue of file-transfer tasks (both completed and ongoing/upcoming)
 *  and execute said tasks (asynchronously) end to end. That is, the end of one task in a queue signals the
 *  next one to start. It should do this for as long as the queue has unfinished tasks in it and the Manager
 *  is not set in a Halt/Pause state.
 */
public class TransferManager {
    private MyFTPActivity mParentActivity;
    private FTPClientWrapper mClient;

    private ArrayList<TransferItem> mItemQueue;
    private ArrayList<FileTransferTask> mTaskQueue;
    private int mQueuePosition;

    public static final int CLIENT = 0;
    public static final int SERVER = 1;

    private boolean PAUSED;

    public TransferManager(MyFTPActivity activity, FTPClientWrapper client) {
        mParentActivity = activity;
        mClient = client;
        mItemQueue = new ArrayList<>();
        mTaskQueue = new ArrayList<>();
        mQueuePosition = 0;
        PAUSED = true;
    }

    public void addItem(String name, String path, long size, String target, int loc) {
        TransferItem item = new TransferItem(name, path, size, target, loc, 0, 0);
        mItemQueue.add(item);

        mTaskQueue.add(new FileTransferTask(mParentActivity, mClient, item));
        mParentActivity.getInterface().printStatus((loc == CLIENT ? "CLIENT:" : "SERVER:") + path + " queued for transfer to " + (loc == CLIENT ? "SERVER:" : "CLIENT:") + target + ".");
        unpause();
    }

    public void removeItem(TransferItem item) {
        int itemIdx = mItemQueue.indexOf(item);
        if (mQueuePosition == itemIdx) {
            mTaskQueue.get(itemIdx).cancel(true);
            mTaskQueue.remove(itemIdx);
            deleteIncompleteTarget(item);
        } else if (mQueuePosition > itemIdx) {
            mTaskQueue.remove(itemIdx);
            mQueuePosition--;
        } else {
            mTaskQueue.remove(itemIdx);
        }
        mItemQueue.remove(item);
        mParentActivity.getInterface().printStatus((item.fileLocation == CLIENT ? "CLIENT:" : "SERVER:") + item.filePath + " removed from transfer queue.");
    }

    public ArrayList<TransferItem> getList() {
        return mItemQueue;
    }

    public void pause() {
        if (!PAUSED) {
            PAUSED = true;
        }
        if (mItemQueue.size() > mQueuePosition) {
            if (mItemQueue.get(mQueuePosition).transferResult == 1) {
                mItemQueue.get(mQueuePosition).transferResult = 2;
                mTaskQueue.get(mQueuePosition).setPaused(true);
            }
        }
        mParentActivity.getInterface().setServerBrowsing(true);
        mParentActivity.getInterface().printStatus("Transfer queue paused.");
    }

    public void unpause() {
        if (PAUSED) {
            if (mItemQueue.get(mQueuePosition).transferResult == 2) {
                mItemQueue.get(mQueuePosition).transferResult = 1;
                mTaskQueue.get(mQueuePosition).setPaused(false);
                PAUSED = false;
            } else if (mItemQueue.get(mQueuePosition).transferResult == 0) {
                mItemQueue.get(mQueuePosition).transferResult = 1;
                mTaskQueue.get(mQueuePosition).execute();
                PAUSED = false;
            }
            if (!PAUSED) {
                mParentActivity.getInterface().setServerBrowsing(false);
            }
            mParentActivity.getInterface().printStatus("Transfer queue unpaused.");
        }
    }

    public boolean getPaused() {
        return PAUSED;
    }

    public void initiateNextTransfer(Integer result) {
        pause();
        mItemQueue.get(mQueuePosition).transferResult = result;
        mParentActivity.clearFromSelected(mItemQueue.get(mQueuePosition));
        mQueuePosition++;
        if (mItemQueue.size() > mQueuePosition) {
            unpause();
        }
    }

    public void clearQueue() {
        pause();
        mClient.returnToHome(); // Return the clients working directory to root
        for (int i = 0; i < mItemQueue.size(); i++) {
            TransferItem item = mItemQueue.get(i);
            FileTransferTask task = mTaskQueue.get(i);
            if (item.transferResult > 0 && item.transferResult < 4) {
                mParentActivity.clearFromSelected(item); // Remove all incomplete items from the SelectedList
                deleteIncompleteTarget(item); // Clear any files that have been created but not fully transferred.
            }
            task.cancel(true); // Cancel all tasks with permission to interrupt
        }
        mItemQueue.clear();
        mTaskQueue.clear();
        mQueuePosition = 0;
    }

    public void updateProgress(Integer percent, Integer numKBytes) {
        mItemQueue.get(mQueuePosition).transferProgress = percent;
    }

    private void deleteIncompleteTarget(TransferItem item) {
        if (item.isClient()) {
            if (checkTargetExists(item.targetPath, SERVER)) {
                mClient.deleteFile(item.fileName, item.targetPath);
            }
        } else {
            File file = new File(item.targetPath, item.fileName);
            if (file.exists()) file.delete();
        }
    }

    private boolean checkSourceExists(File source, int location) {
        if (location == CLIENT) {
            return false; // Place-Holder
        } else if (location == SERVER) {
            return false; // Place-Holder
        } else {
            // TODO: Maybe this case should throw an exception or otherwise make an error known.
            return false;
        }
    }

    private boolean checkTargetExists(String path, int location) {
        if (location == CLIENT) {
            return false; // Place-Holder
        } else if (location == SERVER) {
            return false; // Place-Holder
        } else {
            // TODO: Maybe this case should throw an exception or otherwise make an error known.
            return false;
        }
    }
}
