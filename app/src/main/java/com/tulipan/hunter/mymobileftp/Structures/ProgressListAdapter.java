package com.tulipan.hunter.mymobileftp.Structures;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.R;
import com.tulipan.hunter.mymobileftp.Views.ProgressTab;

import java.util.List;

public class ProgressListAdapter extends ArrayAdapter<TransferItem> {
    ProgressTab mParentTab;

    public ProgressListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public ProgressListAdapter(Context context, int resource, List<TransferItem> items) {
        super(context, resource, items);
    }

    public ProgressListAdapter(Context context, int resource, List<TransferItem> items,
                               ProgressTab parent) {
        super(context, resource, items);
        mParentTab = parent;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final TransferItem item = getItem(position);

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());

            if (item.isClient()) {
                v = vi.inflate(R.layout.progress_client_list_row, null);
            } else {
                v = vi.inflate(R.layout.progress_server_list_row, null);
            }
        }

        if (item != null) {
            TextView fileName = (TextView) v.findViewById(R.id.filename);
            TextView fileSize = (TextView) v.findViewById(R.id.filesize);
            ImageButton pauseButton = (ImageButton) v.findViewById(R.id.pause_button);
            ImageButton removeButton = (ImageButton) v.findViewById(R.id.remove_button);
            ImageView result = (ImageView) v.findViewById(R.id.transfer_result);
            ProgressBar progressbar = (ProgressBar) v.findViewById(R.id.progress_bar);

            fileName.setText(item.fileName);
            StringBuilder sizeDisplay = new StringBuilder();
            sizeDisplay.append(((item.fileSize*item.transferProgress)/100L)/1000L)
                    .append('/')
                    .append(item.fileSize/1000L)
                    .append("kB");

            fileSize.setText(sizeDisplay.toString());
            progressbar.setProgress(item.transferProgress);

            switch (item.transferResult) {
                case 0: // File transfer is still in queue
                    pauseButton.setEnabled(false);
                    result.setVisibility(View.GONE);
                    fileSize.setVisibility(View.VISIBLE);
                    break;

                case 1: // File transfer has started and is ongoing
                    pauseButton.setEnabled(true);
                    pauseButton.setSelected(false);
                    progressbar.setVisibility(View.VISIBLE);
                    result.setVisibility(View.GONE);
                    fileSize.setVisibility(View.VISIBLE);
                    break;

                case 2: // File transfer has started and is paused
                    pauseButton.setEnabled(true);
                    pauseButton.setSelected(true);
                    progressbar.setVisibility(View.VISIBLE);
                    result.setVisibility(View.GONE);
                    fileSize.setVisibility(View.VISIBLE);
                    break;

                case 3: // File transfer is done and an error occurred
                    pauseButton.setEnabled(false);
                    result.setImageDrawable(ContextCompat.getDrawable(getContext(),
                            R.drawable.ic_error_outline_purple_48dp));
                    fileSize.setVisibility(View.GONE);
                    result.setVisibility(View.VISIBLE);
                    break;

                case 4: // File transfer is done and there was no error
                    pauseButton.setEnabled(false);
                    result.setImageDrawable(ContextCompat.getDrawable(getContext(),
                            R.drawable.ic_done_purple_48dp));
                    fileSize.setVisibility(View.GONE);
                    result.setVisibility(View.VISIBLE);
                    break;

                default:
                    pauseButton.setEnabled(false);
                    break;
            }

            if (mParentTab.checkIfPaused()) {
                removeButton.setEnabled(true);
            } else {
                removeButton.setEnabled(false);
            }

            pauseButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (v.isSelected()) {
                        v.setSelected(false);
                        // Resume transfer
                        // This will require a call to TransferManager
                        mParentTab.callResumeTransfer();
                    } else {
                        v.setSelected(true);
                        // Stop transfer
                        // This will require a call to TransferManager
                        mParentTab.callPauseTransfer();
                    }
                }
            });

            removeButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    mParentTab.removeProgressItem(item);
                    mParentTab.updateList();
                }
            });
        }
        return v;
    }
}
