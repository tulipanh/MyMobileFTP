package com.tulipan.hunter.mymobileftp.Structures;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;

import java.io.File;
import java.util.ArrayList;

/**
 * TransferManager keeps track of all information regarding file transfers that have been approved
 * by the user. It is through this class that all FileTransferTasks are created, executed, and
 * destroyed. The class maintains two Lists:
 *      - The ItemQueue is a list of TransferItems, which each contain the name, path, and size
 *      of a file to be transferred, as well as the path of the target, location of the file
 *      (client or server), degree of progress (as a percentage), and an integer indicating the
 *      stage and status of the transfer.
 *      - The TaskQueue is a list of FileTransferTasks which match one-to-one with the items in
 *      the ItemQueue. That is to say, the FileTransferTask at index 1 of the TaskQueue matches
 *      with the TransferItem at index 1 of the ItemQueue.
 * The TransferManager also keeps track of which transfer is ongoing via the QueuePosition, and
 * whether the queue is paused or not.
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

    /**
     * Checks whether the paths given for the file and transfer target are valid and adds an item
     * to each of the ItemQueue and TaskQueue, officially setting the Transfer up for execution.
     * @param name This String contains the name of the file to be transferred.
     * @param path This String contains the filepath of the file to be transferred.
     * @param size This long integer contains the size of the file in Bytes.
     * @param target This String contains the filepath of the directory into which the file will be
     *               transferred.
     * @param loc This integer encodes the location of the file to be transferred, 0 for Client, 1
     *            for Server.
     */
    public void addItem(String name, String path, long size, String target, int loc) {
        TransferItem item = new TransferItem(name, path, size, target, loc, 0, 0);

        if (isValidTransfer(item)) {
            mItemQueue.add(item);
            mTaskQueue.add(new FileTransferTask(mParentActivity, TransferManager.this,
                    mClient, item));
            mParentActivity.getInterface().printStatus((loc == CLIENT ? "CLIENT:" : "SERVER:") +
                    path + " queued for transfer to " + (loc == CLIENT ? "SERVER:" : "CLIENT:") +
                    target + ".");
            unpause();
        } else {
            mParentActivity.getInterface().printError((loc == CLIENT ? "CLIENT:" : "SERVER:") +
                    path + " could not be queued for transfer to " +
                    (loc == CLIENT ? "CLIENT:" : "SERVER:") + target + ".");
        }
    }

    /**
     * Removes a TransferItem and its corresponding FileTransferTask from the queue, whether the
     * transfer is completed, ongoing, or uninitiated.
     * @param item This TransferItem contains all the info to identify a single file transfer, and
     *             should match a TransferItem in ItemQueue.
     */
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
        mParentActivity.getInterface().printStatus((item.fileLocation ==
                CLIENT ? "CLIENT:" : "SERVER:") + item.filePath + " removed from transfer queue.");
    }

    /**
     * Gets the ItemQueue list. This will most often be called from the Progress Tab so it may
     * update its Views with the contained information.
     * @return The ArrayList containing all the TransferItems tracked in the TransferManager.
     */
    public ArrayList<TransferItem> getList() {
        return mItemQueue;
    }

    /**
     * Signals the queue to halt progress and for the currently ongoing file transfer not to read
     * write any more until the queue is unpaused. Also sets the result code of the TransferItem
     * to reflect the paused state and enables server browsing again while the file is not
     * actively transferring.
     */
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

    /**
     * Signals the queue to continue progress and for the file transfer in the current queue
     * position to either initiate or continue reading and writing. Sets the result code of the
     * corresponding TransferItem as well as server browsing ability to reflect this.
     */
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

    /**
     * Gets the current paused or unpaused status of the transfer queue.
     * @return This boolean indicates whether or not the queue is paused.
     */
    public boolean getPaused() {
        return PAUSED;
    }

    /**
     * Signals the queue to step forward and start the next transfer is there is one. Also relays
     * the result of the previous transfer to its TransferItem and tells the Progress Tab to
     * update its Views to reflect these changes.
     * @param result This integer encodes the result of the previous transfer, 4 for successful,
     *               3 for unsuccessful.
     */
    public void initiateNextTransfer(Integer result) {
        pause();
        mItemQueue.get(mQueuePosition).transferResult = result;
        mParentActivity.clearFromSelected(mItemQueue.get(mQueuePosition));
        mQueuePosition++;
        if (mItemQueue.size() > mQueuePosition) {
            unpause();
        }
        mParentActivity.updateProgressList();
    }

    /**
     * Relays the progress of the current transfer to the appropriate data structures and the Views
     * displaying them.
     * @param percent This integer indicates the percent of the transfer which which has been
     *                completed.
     */
    public void postTransferProgress(Integer percent) {
        updateProgress(percent);
        mParentActivity.updateProgressList();
    }

    /**
     * Wraps up all tasks, complete or incomplete, clears the queues of all transfers, and deletes
     * files in target directories whose transfers are incomplete.
     */
    public void clearQueue() {
        pause();
        mClient.returnToHome(); // Return the clients working directory to root
        for (int i = 0; i < mItemQueue.size(); i++) {
            TransferItem item = mItemQueue.get(i);
            FileTransferTask task = mTaskQueue.get(i);
            if (item.transferResult > 0 && item.transferResult < 4) {
                // Remove all incomplete items from the SelectedList
                mParentActivity.clearFromSelected(item);
                // Clear any files that have been created but not fully transferred.
                deleteIncompleteTarget(item);
            }
            task.cancel(true); // Cancel all tasks with permission to interrupt
        }
        mItemQueue.clear();
        mTaskQueue.clear();
        mQueuePosition = 0;
    }

    /**
     * Updates the current TransferItem with the percent completion, report by the the
     * corresponding FileTransferTask.
     * @param percent This integer contains the percent of the file which has been written.
     */
    public void updateProgress(Integer percent) {
        mItemQueue.get(mQueuePosition).transferProgress = percent;
    }

    /**
     * Deletes a file which has been created and partially written to, but not completely
     * transferred
     * @param item This TransferItem contains all info for a transfer which has not been completed.
     */
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

    /**
     * Checks if a file transfer is valid by examining the location of the source file and the
     * target location of the transfer.
     * @param item This TransferItem contains all info for a transfer which is being checked for
     *             validity before being added to the queue.
     * @return This boolean indicates whether the transfer may proceed or not.
     */
    private boolean isValidTransfer(TransferItem item) {
        if (item.isClient()) {
            return checkSourceExists(item.filePath, CLIENT) &&
                    checkTargetExists(item.targetPath, SERVER);
        } else {
            return checkSourceExists(item.filePath, SERVER) &&
                    checkTargetExists(item.targetPath, CLIENT);
        }
    }

    /**
     * Helper function for isValidTransfer, checking if the file chosen for transfer exists and is a
     * file rather than a directory.
     * @param path This String contains the path of the file to be transferred.
     * @param location This integer indicates the location of the file, 0 for Client, 1 for Server.
     * @return This boolean indicates whether or not the source file exists.
     */
    private boolean checkSourceExists(String path, int location) {
        if (location == CLIENT) {
            return new File(path).isFile();
        } else if (location == SERVER) {
            return mClient.getFileInfo(path).isFile();
        } else {
            return false;
        }
    }

    /**
     * Helper function for isValidTransfer, checking if the target chosen for transfer exists and
     * is a directory rather than a file.
     * @param path This String contains the path of the target directory.
     * @param location This integer indicates the location of the directory, 0 for Client,
     *                 1 for Server.
     * @return This boolean indicates whether or not the target directory exists.
     */
    private boolean checkTargetExists(String path, int location) {
        if (location == CLIENT) {
            return new File(path).isDirectory();
        } else if (location == SERVER) {
            return mClient.getFileInfo(path).isDirectory();
        } else {
            return false;
        }
    }
}
