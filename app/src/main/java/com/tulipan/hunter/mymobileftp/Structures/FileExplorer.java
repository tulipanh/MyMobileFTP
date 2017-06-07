package com.tulipan.hunter.mymobileftp.Structures;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Hunter on 12/22/2016.
 */
public class FileExplorer {
    private MyFTPActivity mParentActivity;
    private File mCurrentDirectory;
    private ArrayList<String> mFileList;
    private ArrayList<String> mHistory;

    private static final String TAG = "FileExplorer";

    public FileExplorer(MyFTPActivity parent) {
        mParentActivity = parent;

        /**
         * Check file read permissions. Attempt to get them if you don't have them.
         */
        if (!mParentActivity.checkPermissions()) {
            Log.e(TAG, "Read permissions not granted.",
                    new Exception("Read permissions not granted."));
        }

        /**
         * Set the starting directory.
         */
        setStartFileDir();

        mFileList = new ArrayList<>();
        mHistory = new ArrayList<>();
        /**
         * Get file names.
         */
        fetchFileNames();

    }

    private void setStartFileDir() {
        if (isExternalStorageReadable()) {
            mCurrentDirectory = new File("/");

                    //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            Log.e(TAG, "Couldn't find starting directory.");
        }
    }

    private void fetchFileNames() {
        String[] fileNames = mCurrentDirectory.list();
        mFileList.clear();
        if (fileNames != null) mFileList.addAll(Arrays.asList(fileNames));
    }

    public String getCurrentName() {
        return mCurrentDirectory.getName();
    }

    public String getCurrentPath() {
        return mCurrentDirectory.getPath();
    }

    public String getCurrentAbsolutePath() {
        return mCurrentDirectory.getAbsolutePath();
    }

    public ArrayList<String> getFileNames() {
        ArrayList<String> list = new ArrayList<>();
        for (String s : mFileList) list.add(s);
        return list;
    }

    public ArrayList<File> getFileList() {
        ArrayList<File> files = new ArrayList<>();
        for (String filename : mFileList) {
            files.add(new File(mCurrentDirectory, filename));
        }
        return files;
    }

    public HashMap<String, File> getFileMap() {
        HashMap<String, File> filemap = new HashMap<>();
        for (String filename : mFileList) {
            filemap.put(filename, new File(mCurrentDirectory, filename));
        }
        return filemap;
    }

    public void setChildToCurrent(String dirName) {
        if (mFileList.contains(dirName)) {
            File target = new File(mCurrentDirectory, dirName);
            if (target.isDirectory()) {
                setCurrentDirectory(target);
            } else {
                Toast.makeText(mParentActivity, "Target file is not a directory.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(mParentActivity, dirName + " is not in the current directory.", Toast.LENGTH_LONG).show();
        }
    }

    public void goBack() {
        String lastDirPath = mHistory.get(mHistory.size()-1);
        mHistory.remove(mHistory.size()-1);
        mCurrentDirectory = new File(lastDirPath);
        fetchFileNames();
    }

    public void setParentToCurrent() {
        if (mCurrentDirectory.getAbsolutePath().equals("/")) {
            Toast.makeText(mParentActivity, "This directory has no parent.", Toast.LENGTH_LONG).show();
        } else {
            setCurrentDirectory(mCurrentDirectory.getParentFile());
        }
    }

    public void setCurrentDirectory(File target) {
        mHistory.add(mCurrentDirectory.getAbsolutePath());
        mCurrentDirectory = target;
        fetchFileNames();
    }

    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return (Environment.MEDIA_MOUNTED.equals(state)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state));
    }
}
