package com.szte.tudastenger.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.R;
import com.szte.tudastenger.models.User;


import java.util.ArrayList;


public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {
    private ArrayList<User> mFriends;
    private Context mContext;

    public FriendAdapter(Context context, ArrayList<User> friends){
        this.mFriends = friends;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_friends, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User currentUser = mFriends.get(position);
        holder.bindTo(currentUser);
    }

    @Override
    public int getItemCount() {
        return mFriends.size();
    }
    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView mUsernameText;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mUsernameText = itemView.findViewById(R.id.listFriendUsernameTextView);
        }

        public void bindTo(User currentUser) {
            mUsernameText.setText(currentUser.getUsername());
        }
    }
}

