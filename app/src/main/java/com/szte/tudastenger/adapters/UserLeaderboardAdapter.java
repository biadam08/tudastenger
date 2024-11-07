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
import com.szte.tudastenger.models.LeaderboardEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserLeaderboardAdapter extends RecyclerView.Adapter<UserLeaderboardAdapter.ViewHolder> {
    private List<LeaderboardEntry> mUserList;
    private Context mContext;

    public UserLeaderboardAdapter(Context context, List<LeaderboardEntry> userList){
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
        LeaderboardEntry userEntry = mUserList.get(position);
        holder.bindTo(userEntry, position);
    }

    @Override
    public int getItemCount() {
        if(mUserList != null) {
            return mUserList.size();
        }
        return 0;
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

        public void bindTo(LeaderboardEntry userEntry, int position) {
            String orderText = position + 1 + ".";
            orderNumber.setText(orderText);

            String text = userEntry.getUsername();
            if(userEntry.getRank() != null && !Objects.equals(userEntry.getRank(), "")){
                text += " (" + userEntry.getRank() + ")";
            }
            usernameTextView.setText(text);

            pointsTextView.setText(String.valueOf(userEntry.getPoints()));
        }
    }
    public void updateData(List<LeaderboardEntry> newEntries) {
        this.mUserList = newEntries;
        notifyDataSetChanged();
    }
}

