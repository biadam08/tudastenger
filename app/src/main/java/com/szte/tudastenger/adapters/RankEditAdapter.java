package com.szte.tudastenger.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.szte.tudastenger.R;
import com.szte.tudastenger.activities.RankEditUploadActivity;
import com.szte.tudastenger.models.Rank;

import java.util.ArrayList;

public class RankEditAdapter extends RecyclerView.Adapter<RankEditAdapter.ViewHolder> {
    private ArrayList<Rank> mRanksData;
    private Context mContext;

    public RankEditAdapter(Context context, ArrayList<Rank> ranksData) {
        this.mRanksData = ranksData;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_edit_rank, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Rank currentRank = mRanksData.get(position);
        holder.bindTo(currentRank);
    }

    @Override
    public int getItemCount() {
        return mRanksData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView rankName;
        private TextView threshold;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankName = itemView.findViewById(R.id.rankNameTextView);
            threshold = itemView.findViewById(R.id.thresholdTextView);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        Rank clickedRank = mRanksData.get(position);
                        Intent intent = new Intent(mContext, RankEditUploadActivity.class);
                        intent.putExtra("rankId", clickedRank.getId());
                        mContext.startActivity(intent);
                    }
                }
            });
        }

        public void bindTo(Rank currentRank) {
            rankName.setText(currentRank.getRankName());
            String thresholdText = currentRank.getThreshold().toString() + " pont";
            threshold.setText(thresholdText);
        }
    }
}
