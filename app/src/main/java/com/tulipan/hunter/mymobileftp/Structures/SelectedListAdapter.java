package com.tulipan.hunter.mymobileftp.Structures;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.R;
import com.tulipan.hunter.mymobileftp.Views.SelectedTab;

import java.util.List;

/**
 * Created by Hunter on 4/11/2017.
 */
public class SelectedListAdapter extends ArrayAdapter<SelectedListItem> {
    SelectedTab mParentTab;

    public SelectedListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public SelectedListAdapter(Context context, int resource, List<SelectedListItem> items) {
        super(context, resource, items);
    }

    public SelectedListAdapter(Context context, int resource, List<SelectedListItem> items, SelectedTab parent) {
        super(context, resource, items);
        mParentTab = parent;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        final SelectedListItem item = getItem(position);

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            if (item.isClient()) {
                v = vi.inflate(R.layout.selected_client_file_row, null);
            } else {
                v = vi.inflate(R.layout.selected_server_file_row, null);
            }
        }

        if (item != null) {
            TextView nameView = (TextView) v.findViewById(R.id.filename);
            TextView sizeView = (TextView) v.findViewById(R.id.filesize);
            ImageButton transferButton = null;
            ImageButton removeButton = null;

            transferButton = (ImageButton) v.findViewById(R.id.selected_transfer_button);
            removeButton = (ImageButton) v.findViewById(R.id.selected_remove_button);

            if (nameView != null) {
                nameView.setText(item.getName());
            }

            if (sizeView != null) {
                long size = item.getSize();
                long kilosize = size / 1000L;
                sizeView.setText(String.valueOf(kilosize) + " kB");
            }

            if (transferButton != null) {
                if (item.isClient()) {
                    transferButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mParentTab.handToProgressTab(item);
                            v.setEnabled(false);
                        }
                    });
                } else {
                    transferButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mParentTab.handToProgressTab(item);
                            v.setEnabled(false);
                        }
                    });
                }
            }

            if (removeButton != null) {
                if (item.isClient()) {
                    removeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            item.deselect();
                        }
                    });
                } else {
                    removeButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            item.deselect();
                        }
                    });
                }
            }
        }

        return v;
    }
}