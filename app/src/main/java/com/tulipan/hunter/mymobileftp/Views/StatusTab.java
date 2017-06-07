package com.tulipan.hunter.mymobileftp.Views;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tulipan.hunter.mymobileftp.MyFTPActivity;
import com.tulipan.hunter.mymobileftp.R;
import com.tulipan.hunter.mymobileftp.Structures.StatusMessage;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Hunter on 12/13/2016.
 */

public class StatusTab {
    private MyFTPActivity mParentActivity;
    private LinearLayout mButton;
    private RecyclerView mRecyclerView;
    private StatusAdapter mAdapter;

    public StatusTab(MyFTPActivity activity, LinearLayout button, RecyclerView recview) {
        mParentActivity = activity;
        mButton = button;
        mRecyclerView = recview;

        mRecyclerView.setLayoutManager(new LinearLayoutManager(mParentActivity, LinearLayoutManager.VERTICAL, false));
        mAdapter = new StatusAdapter();
        mRecyclerView.setAdapter(mAdapter);

    }

    private class StatusHolder extends RecyclerView.ViewHolder {
        private TextView mText;
        private TextView mTime;
        private StatusMessage mMessage;
        private String mMessageString;
        private Integer mTypeCode;
        private String mTimeCode;

        public StatusHolder(LayoutInflater inflater, ViewGroup container) {
            super(inflater.inflate(R.layout.status_list_item, container, false));

            mText = (TextView) itemView.findViewById(R.id.status_list_item_text);
            mTime = (TextView) itemView.findViewById(R.id.status_list_item_timecode);
        }

        public void bindMessage(StatusMessage message) {
            mMessage = message;
            mMessageString = mMessage.message;
            mTypeCode = mMessage.type;
            mTimeCode = mMessage.time;
            mText.setText(mMessageString);
            mTime.setText(mTimeCode);

            switch (mTypeCode) {
                case 3:
                    mText.setBackgroundColor(ContextCompat.getColor(mParentActivity, R.color.colorError));
                    break;
                case 2:
                    mText.setBackgroundColor(ContextCompat.getColor(mParentActivity, R.color.colorWarning));
                    break;
                case 1:
                    mText.setBackgroundColor(ContextCompat.getColor(mParentActivity, R.color.colorGood));
                    break;
                default:
                    break;
            }
        }
    }

    private class StatusAdapter extends RecyclerView.Adapter<StatusHolder> {
        private ArrayList<StatusMessage> mMessages;

        public StatusAdapter() {
            mMessages = new ArrayList<StatusMessage>();
        }

        @Override
        public StatusHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mParentActivity);
            return new StatusHolder(inflater, parent);
        }

        @Override
        public void onBindViewHolder(StatusHolder holder, int position) {
            StatusMessage message = mMessages.get(position);
            holder.bindMessage(message);
        }

        @Override
        public int getItemCount() {
            return mMessages.size();
        }

        public void addMessage(String m, Integer t) {
            mMessages.add(new StatusMessage(m, t));
        }
    }

    public void addError(String message) {
        mAdapter.addMessage(message, 3);
        mAdapter.notifyItemInserted(mAdapter.getItemCount() - 1);
    }

    public void addWarning(String message) {
        mAdapter.addMessage(message, 2);
        mAdapter.notifyItemInserted(mAdapter.getItemCount() - 1);
    }

    public void addStatus(String message) {
        mAdapter.addMessage(message, 1);
        mAdapter.notifyItemInserted(mAdapter.getItemCount() - 1);
    }

    public void addSentMessage(String message) {
        mAdapter.addMessage(message, 0);
        mAdapter.notifyItemInserted(mAdapter.getItemCount() - 1);
    }

    public void setSelected(boolean isSelected) {
        mButton.setSelected(isSelected);
        if (isSelected) {
            mRecyclerView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
        }
    }
}
