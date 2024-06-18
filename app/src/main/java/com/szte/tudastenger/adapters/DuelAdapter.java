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
import com.szte.tudastenger.activities.DuelActivity;
import com.szte.tudastenger.models.Duel;

import java.util.ArrayList;

public class DuelAdapter extends RecyclerView.Adapter<DuelAdapter.ViewHolder> {
    private ArrayList<Duel> mDuelsData;
    private Context mContext;

    public DuelAdapter(Context context, ArrayList<Duel> duelsData){
        this.mDuelsData = duelsData;
        this.mContext = context;
    }

    @NonNull
    @Override
    public DuelAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DuelAdapter.ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.list_pending_duels, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull DuelAdapter.ViewHolder holder, int position) {
        Duel currentDuel = mDuelsData.get(position);
        holder.bindTo(currentDuel);
    }

    @Override
    public int getItemCount() {
        return mDuelsData.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mDuelName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mDuelName = itemView.findViewById(R.id.duelName);
        }

        public void bindTo(Duel currentDuel) {
            mDuelName.setText(currentDuel.getId());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(mContext, DuelActivity.class);
                    intent.putExtra("duelId", currentDuel.getId());
                    mContext.startActivity(intent);
                }
            });
        }
    }
}
