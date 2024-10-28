package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.activities.QuizGameActivity;
import com.szte.tudastenger.R;
import com.szte.tudastenger.models.Category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UserLeaderboardAdapter extends RecyclerView.Adapter<UserLeaderboardAdapter.ViewHolder> {
    private List<Map.Entry<String, Integer>> mUserList;
    private Context mContext;

    public UserLeaderboardAdapter(Context context, List<Map.Entry<String, Integer>> userList){
        this.mUserList = userList;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_user_on_leaderboard, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Integer> userEntry = mUserList.get(position);
        holder.bindTo(userEntry, position);
    }

    @Override
    public int getItemCount() {
        return mUserList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder{
        private TextView orderNumber;
        private TextView usernameTextView;
        private TextView pointsTextView;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderNumber = itemView.findViewById(R.id.orderNumber);
            usernameTextView = itemView.findViewById(R.id.usernameTextView);
            pointsTextView = itemView.findViewById(R.id.pointsTextView);
        }

        public void bindTo(Map.Entry<String, Integer> userEntry, int position) {
            orderNumber.setText(String.valueOf(position+1) + ".");
            usernameTextView.setText(userEntry.getKey());
            pointsTextView.setText(String.valueOf(userEntry.getValue()));
        }
    }
    public void updateData(List<Map.Entry<String, Integer>> newData) {
        mUserList.clear();
        mUserList.addAll(newData);
        notifyDataSetChanged();
    }
}

