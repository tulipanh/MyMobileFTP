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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.R;

import java.util.List;

/**
 * Created by Hunter on 4/10/2017.
 */
public class FileListAdapter extends ArrayAdapter<FileListItem> {
    private FileSelector mParentFragment;

    public FileListAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public FileListAdapter(Context context, int resource, List<FileListItem> items) {
        super(context, resource, items);
    }

    public FileListAdapter(Context context, int resource, List<FileListItem> items, FileSelector parent) {
        super(context, resource, items);
        mParentFragment = parent;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v ==  null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(mParentFragment.getListItemLayout(), null);
        }

        final FileListItem item = getItem(position);

        if (item != null) {
            LinearLayout background = (LinearLayout) v.findViewById(R.id.filebackground);
            TextView nameView = (TextView) v.findViewById(R.id.filename);
            ImageView itemIcon = (ImageView) v.findViewById(R.id.fileicon);
            ImageButton selectedButton = (ImageButton) v.findViewById(R.id.fileselected);

            if (background != null) {
                if (item.isDirectory()) {
                    background.setBackgroundResource(R.drawable.file_directory_background);
                } else {
                    background.setBackgroundResource(R.color.trueWhite);
                }
            }

            if (nameView != null) {
                nameView.setText(item.getName());
                nameView.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View v) {
                        if (item.isDirectory()) {
                            mParentFragment.navigateToFile(item.getPath());
                        }
                    }
                });
            }

            if (itemIcon != null) {
                if (item.isDirectory()) {
                    itemIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_folder_white_48dp));
                }
                else itemIcon.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_insert_drive_file_white_48dp));
            }

            if (selectedButton != null) {
                if (item.isDirectory()) {
                    selectedButton.setEnabled(false);
                    selectedButton.setSelected(false);
                } else {
                    selectedButton.setEnabled(true);
                    if (item.isSelected()) selectedButton.setSelected(true);
                    else selectedButton.setSelected(false);
                }
                selectedButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        if (item.isDirectory()) return;
                        // TODO: At a later date, need to select or deselect all files in directories recursively.
                        if (item.isSelected()) {
                            mParentFragment.setItemSelected(item.getPath(), false);
                            item.setSelected(false);
                            v.setSelected(false);
                        } else {
                            mParentFragment.setItemSelected(item.getPath(), true);
                            item.setSelected(true);
                            v.setSelected(true);
                        }
                    }
                });
            }
        }

        return v;
    }
}
